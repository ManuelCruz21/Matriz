package pt.matriz.auth;

import java.io.IOException;
import java.time.Clock;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

final class AbsoluteSessionExpiryFilter extends OncePerRequestFilter {
    static final String SESSION_START = "MATRIZ_SESSION_START";
    private static final long MAX_SESSION_SECONDS = 8 * 60 * 60;
    private final Clock clock;

    AbsoluteSessionExpiryFilter(Clock clock) {
        this.clock = clock;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        var session = request.getSession(false);
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (session != null && authentication != null && authentication.isAuthenticated()) {
            Object started = session.getAttribute(SESSION_START);
            if (!(started instanceof Long start)
                    || clock.instant().getEpochSecond() - start >= MAX_SESSION_SECONDS) {
                session.invalidate();
                SecurityContextHolder.clearContext();
                pt.matriz.ApiProblems.write(response, HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
