package pt.matriz.microsoft;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/connections/microsoft")
public class MicrosoftConnectionController {
    private final MicrosoftConnectionService service;
    private final JdbcTemplate jdbc;

    public MicrosoftConnectionController(MicrosoftConnectionService service, JdbcTemplate jdbc) {
        this.service = service; this.jdbc = jdbc;
    }

    @ModelAttribute
    void privateResponse(jakarta.servlet.http.HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Referrer-Policy", "no-referrer");
    }

    @GetMapping
    public ResponseEntity<?> status(Authentication auth) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.status(user(auth)));
    }

    @PostMapping("/connect")
    public ResponseEntity<?> connect(Authentication auth, HttpServletRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
            .body(Map.of("authorizationUrl", service.connect(user(auth), request.getSession().getId())));
    }

    @GetMapping("/callback")
    public ResponseEntity<?> callback(Authentication auth, HttpServletRequest request,
            @RequestParam(required = false) String state, @RequestParam(required = false) String code,
            @RequestParam(required = false) String error) {
        var session = request.getSession(false);
        if (session == null) return pt.matriz.ApiProblems.response(401);
        service.callback(user(auth), session.getId(), state, code, error, () -> {
            try {
                // Accessing an invalidated session throws; never create a replacement session.
                var started = session.getAttribute("MATRIZ_SESSION_START");
                return started instanceof Long start
                    && java.time.Instant.now().getEpochSecond() - start < 8 * 60 * 60;
            } catch (IllegalStateException ex) {
                return false;
            }
        });
        // No redirect or provider-supplied text, codes, tokens or identity in the response.
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore())
            .header("Referrer-Policy", "no-referrer").build();
    }

    @RequestMapping(method = {RequestMethod.DELETE, RequestMethod.POST}, path = "/disconnect")
    public ResponseEntity<?> disconnect(Authentication auth) {
        service.disconnect(user(auth));
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }

    private long user(Authentication auth) {
        return jdbc.queryForObject("SELECT id FROM app_users WHERE lower(email) = lower(?)",
            Long.class, auth.getName());
    }
}
