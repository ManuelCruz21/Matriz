package pt.matriz.auth;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;

class AbsoluteSessionExpiryFilterTest {
    private static final Instant NOW = Instant.parse("2026-11-12T12:00:00Z");
    private final AbsoluteSessionExpiryFilter filter =
            new AbsoluteSessionExpiryFilter(Clock.fixed(NOW, ZoneOffset.UTC));

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void allowsSessionImmediatelyBeforeEightHoursWithoutExtendingItsStart() throws Exception {
        long start = NOW.getEpochSecond() - 8 * 60 * 60 + 1;
        var request = request(start);
        var chain = new MockFilterChain();
        filter.doFilter(request, new MockHttpServletResponse(), chain);
        assertNotNull(chain.getRequest());
        assertEquals(start, request.getSession().getAttribute(AbsoluteSessionExpiryFilter.SESSION_START));
        assertEquals(30 * 60, request.getSession().getMaxInactiveInterval());
    }

    @Test
    void expiresExactlyAtEightHoursDespiteRecentActivity() throws Exception {
        var request = request(NOW.getEpochSecond() - 8 * 60 * 60);
        var session = (MockHttpSession) request.getSession();
        session.access();
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();
        filter.doFilter(request, response, chain);
        assertEquals(401, response.getStatus());
        assertTrue(session.isInvalid());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNull(chain.getRequest());
    }

    @Test
    void rejectsMalformedSessionStart() throws Exception {
        var request = request("invalid");
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        assertEquals(401, response.getStatus());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void anonymousRequestDoesNotCreateSession() throws Exception {
        var request = new MockHttpServletRequest();
        var chain = new MockFilterChain();
        filter.doFilter(request, new MockHttpServletResponse(), chain);
        assertNull(request.getSession(false));
        assertNotNull(chain.getRequest());
    }

    private MockHttpServletRequest request(Object start) {
        var request = new MockHttpServletRequest();
        var session = new MockHttpSession();
        session.setMaxInactiveInterval(30 * 60);
        session.setAttribute(AbsoluteSessionExpiryFilter.SESSION_START, start);
        request.setSession(session);
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("gestor@example.org", null, java.util.List.of()));
        return request;
    }
}
