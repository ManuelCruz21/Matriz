package pt.matriz.auth;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
    @Bean UserDetailsService userDetailsService(JdbcTemplate jdbc) {
        return username -> {
            var users = jdbc.query("SELECT email, password_hash FROM app_users WHERE lower(email) = lower(?)",
                (rs, row) -> User.withUsername(rs.getString("email"))
                    .password(rs.getString("password_hash")).authorities("USER").build(), username);
            if (users.isEmpty()) throw new org.springframework.security.core.userdetails.UsernameNotFoundException("Invalid credentials");
            return users.get(0);
        };
    }
    @Bean AuthenticationManager authenticationManager(UserDetailsService users, PasswordEncoder encoder) {
        var provider = new DaoAuthenticationProvider(users);
        provider.setPasswordEncoder(encoder);
        return new ProviderManager(provider);
    }
    @Bean HttpSessionSecurityContextRepository securityContextRepository() { return new HttpSessionSecurityContextRepository(); }
    @Bean CsrfTokenRepository csrfTokenRepository() {
        var repo = new HttpSessionCsrfTokenRepository();
        repo.setHeaderName("X-CSRF-TOKEN");
        return repo;
    }
    @Bean SecurityFilterChain filterChain(HttpSecurity http, CsrfTokenRepository csrf,
                                          HttpSessionSecurityContextRepository contexts) throws Exception {
        http.addFilterAfter(new AbsoluteSessionExpiryFilter(java.time.Clock.systemUTC()),
                org.springframework.security.web.context.SecurityContextHolderFilter.class)
            .csrf(c -> c.csrfTokenRepository(csrf))
            .securityContext(c -> c.securityContextRepository(contexts))
            .authorizeHttpRequests(a -> a
                .requestMatchers(HttpMethod.GET, "/api/health", "/api/v1/auth/csrf").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                .anyRequest().authenticated())
            .exceptionHandling(e -> e
                .authenticationEntryPoint((req, res, ex) -> pt.matriz.ApiProblems.write(res, 401))
                .accessDeniedHandler((req, res, ex) -> {
                    var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                    boolean authenticated = auth != null && auth.isAuthenticated()
                        && !(auth instanceof org.springframework.security.authentication.AnonymousAuthenticationToken);
                    boolean login = "POST".equals(req.getMethod()) && (req.getContextPath() + "/api/v1/auth/login").equals(req.getRequestURI());
                    pt.matriz.ApiProblems.write(res, authenticated || login ? 403 : 401);
                }))
            .formLogin(f -> f.disable())
            .httpBasic(b -> b.disable())
            .logout(l -> l.disable())
            .sessionManagement(s -> s.sessionFixation(f -> f.changeSessionId()));
        return http.build();
    }
}
