
package pt.matriz.auth;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
class AuthenticationFlowTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private JdbcTemplate jdbc;

    @MockitoBean
    private LoginLimiter limiter;

    @MockitoBean
    private AuthenticationManager manager;

    @Test
    void managerCanLoginViewProfileAndLogout() throws Exception {
        testAuthenticationFlow(
            "gestor@example.org",
            "Gestor Demo",
            1L
        );
    }

    @Test
    void memberCanLoginViewProfileAndLogout() throws Exception {
        testAuthenticationFlow(
            "membro@example.org",
            "Membro Demo",
            2L
        );
    }

    private void testAuthenticationFlow(
        String email,
        String name,
        long id
    ) throws Exception {

        // Simular a autenticação bem-sucedida.
        when(manager.authenticate(any()))
            .thenReturn(
                new UsernamePasswordAuthenticationToken(
                    email,
                    null,
                    List.of(new SimpleGrantedAuthority("USER"))
                )
            );

        // Simular o perfil devolvido pela base de dados.
        when(jdbc.queryForObject(
            anyString(),
            any(org.springframework.jdbc.core.RowMapper.class),
            eq(email)
        )).thenReturn(new AuthController.Profile(id, name, email));

        // Criar uma sessão e obter o token CSRF.
        MvcResult csrfResult = mvc.perform(
                get("/api/v1/auth/csrf")
            )
            .andExpect(status().isOk())
            .andReturn();

        HttpSession session = csrfResult.getRequest().getSession();

        // Usar o token associado à sessão.
        String token = csrfResult.getResponse()
            .getContentAsString();

        String csrfToken = new com.fasterxml.jackson.databind.ObjectMapper()
            .readTree(token)
            .get("token")
            .asText();

        String originalSessionId = session.getId();
        long beforeLogin = java.time.Instant.now().getEpochSecond();

        // Efetuar login.
        MvcResult loginResult = mvc.perform(
                post("/api/v1/auth/login")
                    .session((org.springframework.mock.web.MockHttpSession) session)
                    .header("X-CSRF-TOKEN", csrfToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "username": "%s",
                            "password": "DemoPassword123!"
                        }
                        """.formatted(email))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value(email))
            .andExpect(jsonPath("$.name").value(name))
            .andReturn();

        // Consultar o perfil com a sessão autenticada.
        var authenticatedSession =
            (org.springframework.mock.web.MockHttpSession)
                loginResult.getRequest().getSession();

        org.junit.jupiter.api.Assertions.assertNotEquals(originalSessionId, authenticatedSession.getId());
        org.junit.jupiter.api.Assertions.assertEquals(30 * 60, authenticatedSession.getMaxInactiveInterval());
        long started = (Long) authenticatedSession.getAttribute(AbsoluteSessionExpiryFilter.SESSION_START);
        org.junit.jupiter.api.Assertions.assertTrue(started >= beforeLogin);
        org.junit.jupiter.api.Assertions.assertTrue(started <= java.time.Instant.now().getEpochSecond());

        mvc.perform(
                get("/api/v1/users/me")
                    .session(authenticatedSession)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value(email));

        // Obter um novo token CSRF após o login.
        MvcResult newCsrf = mvc.perform(
                get("/api/v1/auth/csrf")
                    .session(authenticatedSession)
            )
            .andExpect(status().isOk())
            .andReturn();

        String logoutToken = new com.fasterxml.jackson.databind.ObjectMapper()
            .readTree(newCsrf.getResponse().getContentAsString())
            .get("token")
            .asText();

        // Terminar a sessão.
        mvc.perform(
                post("/api/v1/auth/logout")
                    .session(authenticatedSession)
                    .header("X-CSRF-TOKEN", logoutToken)
            )
            .andExpect(status().isNoContent());

        // Verificar que o perfil já não está acessível.
        mvc.perform(
                get("/api/v1/users/me")
            )
            .andExpect(status().isUnauthorized());
    }
}
