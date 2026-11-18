package pt.matriz.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class AuthController {
    private final AuthenticationManager manager;
    private final HttpSessionSecurityContextRepository contexts;
    private final CsrfTokenRepository csrfRepo;
    private final JdbcTemplate jdbc;
    private final LoginLimiter limiter;

    public AuthController(AuthenticationManager manager, HttpSessionSecurityContextRepository contexts,
                          CsrfTokenRepository csrfRepo, JdbcTemplate jdbc, LoginLimiter limiter) {
        this.manager = manager; this.contexts = contexts; this.csrfRepo = csrfRepo;
        this.jdbc = jdbc; this.limiter = limiter;
    }
    public record LoginRequest(String username, String password) {}
    @org.springframework.beans.factory.annotation.Value("${MATRIZ_SECURITY_ADMIN_EMAIL:}")
    private String securityAdminEmail = "";

    public record Profile(long id, String name, String email, boolean securityAdmin) {
        public Profile(long id, String name, String email) { this(id, name, email, false); }
    }

    private boolean isSecurityAdmin(String email) {
        return email != null && !securityAdminEmail.isBlank()
            && securityAdminEmail.trim().equalsIgnoreCase(email);
    }

    @GetMapping("/security/access")
    public ResponseEntity<?> securityAccess(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) return pt.matriz.ApiProblems.response(401);
        if (!isSecurityAdmin(auth.getName())) return pt.matriz.ApiProblems.response(403);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of("allowed", true));
    }

    @GetMapping("/auth/csrf")
    public ResponseEntity<Map<String,String>> csrf(CsrfToken token, HttpServletRequest request) {
        request.getSession(true);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
            .body(Map.of("token", token.getToken(), "headerName", "X-CSRF-TOKEN"));
    }

    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest body, HttpServletRequest request, HttpServletResponse response) {
        if (body == null || body.username() == null || body.password() == null || body.username().isBlank() || body.password().isEmpty())
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid login request"));
        String key = request.getRemoteAddr() + ":" + body.username().toLowerCase(java.util.Locale.ROOT);
        if (limiter.blocked(key)) return ResponseEntity.status(429).body(Map.of("error", "Too many attempts"));
        try {
            Authentication authenticated = manager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(body.username(), body.password()));
            limiter.success(key);
            HttpSession session = request.getSession(true);
            request.changeSessionId();
            session.setMaxInactiveInterval(30 * 60);
            session.setAttribute(AbsoluteSessionExpiryFilter.SESSION_START, Instant.now().getEpochSecond());
            var context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authenticated);
            SecurityContextHolder.setContext(context);
            contexts.saveContext(context, request, response);
            csrfRepo.saveToken(null, request, response);
            return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(profile(authenticated.getName()));
        } catch (BadCredentialsException ex) {
            limiter.failure(key);
            return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
        }
    }

    @GetMapping("/users/me")
    public ResponseEntity<?> me(Authentication auth, HttpServletRequest request) {
        if (auth == null || !auth.isAuthenticated()) return pt.matriz.ApiProblems.response(401);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(profile(auth.getName()));
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<?> logout(Authentication auth, HttpServletRequest request, HttpServletResponse response) {
        if (auth == null || !auth.isAuthenticated()) return pt.matriz.ApiProblems.response(401);
        var session = request.getSession(false);
        if (session != null) session.invalidate();
        SecurityContextHolder.clearContext();
        response.addHeader("Set-Cookie", "JSESSIONID=; Path=/; HttpOnly; SameSite=Lax; Max-Age=0");
        return ResponseEntity.noContent().build();
    }

    private Profile profile(String email) {
        Profile user = jdbc.queryForObject("SELECT id, name, email FROM app_users WHERE lower(email) = lower(?)",
            (rs, row) -> new Profile(rs.getLong("id"), rs.getString("name"), rs.getString("email")), email);
        return new Profile(user.id(), user.name(), user.email(), isSecurityAdmin(user.email()));
    }
}
