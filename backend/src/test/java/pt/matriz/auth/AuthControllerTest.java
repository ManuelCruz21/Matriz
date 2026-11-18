
package pt.matriz.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean private AuthenticationManager manager;
    @MockitoBean private HttpSessionSecurityContextRepository contexts;
    @MockitoBean private CsrfTokenRepository csrfRepo;
    @MockitoBean private JdbcTemplate jdbc;
    @MockitoBean private LoginLimiter limiter;

    @Test
    void rejectsInvalidLogin() throws Exception {
        when(manager.authenticate(any()))
            .thenThrow(new BadCredentialsException("Invalid credentials"));

        mvc.perform(post("/api/v1/auth/login")
                .contentType("application/json")
                .content("""
                    {
                      "username": "gestor@example.org",
                      "password": "wrong-password"
                    }
                    """))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
            .andExpect(jsonPath("$.detail").value("Invalid credentials"));

        verify(limiter).failure(anyString());
    }

    @Test
    void rateLimitedLoginReturnsProblemWithoutAuthenticating() throws Exception {
        when(limiter.blocked(anyString())).thenReturn(true);
        mvc.perform(post("/api/v1/auth/login").contentType("application/json")
                .content("{\"username\":\"gestor@example.org\",\"password\":\"password\"}"))
            .andExpect(status().isTooManyRequests())
            .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
            .andExpect(jsonPath("$.type").value("about:blank"))
            .andExpect(jsonPath("$.title").value("Too Many Requests"))
            .andExpect(jsonPath("$.status").value(429))
            .andExpect(jsonPath("$.detail").value("Too many attempts"));
        verifyNoInteractions(manager, contexts, csrfRepo, jdbc);
        verify(limiter, never()).success(anyString());
    }

    @Test
    void rejectsMissingPassword() throws Exception {
        mvc.perform(post("/api/v1/auth/login")
                .contentType("application/json")
                .content("""
                    {
                      "username": "gestor@example.org",
                      "password": ""
                    }
                    """))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(manager);
    }

    @Test
    void rejectsUnauthenticatedProfile() throws Exception {
        mvc.perform(get("/api/v1/users/me"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsUnauthenticatedLogout() throws Exception {
        mvc.perform(post("/api/v1/auth/logout"))
            .andExpect(status().isUnauthorized());
    }
}
