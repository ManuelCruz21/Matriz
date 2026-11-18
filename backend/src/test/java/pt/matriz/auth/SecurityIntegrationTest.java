
package pt.matriz.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {AuthController.class,
        pt.matriz.projects.ProjectController.class, pt.matriz.tasks.TaskController.class})
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private JdbcTemplate jdbc;

    @MockitoBean
    private LoginLimiter limiter;

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {
        "/api/v1/users/me", "/api/v1/projects", "/api/v1/projects/1",
        "/api/v1/projects/1/tasks", "/api/v1/projects/1/eligible-users", "/api/v1/tasks/1",
        "/api/v1/tasks/1/comments", "/api/v1/tasks/1/history", "/api/v1/dashboard"
    })
    void expiredSessionCannotAccessAnyProtectedResource(String path) throws Exception {
        var session = authenticatedSession();
        session.setAttribute(AbsoluteSessionExpiryFilter.SESSION_START,
                java.time.Instant.now().getEpochSecond() - 8 * 60 * 60);

        mvc.perform(get(path).session(session))
            .andExpect(status().isUnauthorized());

        org.junit.jupiter.api.Assertions.assertTrue(session.isInvalid());
        org.mockito.Mockito.verifyNoInteractions(jdbc);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {
        "/api/v1/projects", "/api/v1/projects/1/tasks", "/api/v1/tasks/1/comments",
        "/api/v1/auth/logout"
    })
    void expiredSessionCannotMutateResourcesEvenWithValidCsrf(String path) throws Exception {
        var session = authenticatedSession();
        session.setAttribute(AbsoluteSessionExpiryFilter.SESSION_START,
                java.time.Instant.now().getEpochSecond() - 8 * 60 * 60);

        mvc.perform(post(path).session(session)
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                .contentType("application/json").content("{}"))
            .andExpect(status().isUnauthorized());

        org.junit.jupiter.api.Assertions.assertTrue(session.isInvalid());
        org.mockito.Mockito.verifyNoInteractions(jdbc);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({
        "PATCH,/api/v1/projects/1", "DELETE,/api/v1/projects/1",
        "PATCH,/api/v1/tasks/1", "DELETE,/api/v1/tasks/1",
        "PATCH,/api/v1/tasks/1/status", "PATCH,/api/v1/tasks/1/priority",
        "PUT,/api/v1/tasks/1/responsibles"
    })
    void expiryAlsoProtectsPutPatchAndDelete(String method, String path) throws Exception {
        var session = authenticatedSession();
        session.setAttribute(AbsoluteSessionExpiryFilter.SESSION_START,
                java.time.Instant.now().getEpochSecond() - 8 * 60 * 60);
        mvc.perform(request(org.springframework.http.HttpMethod.valueOf(method), path).session(session)
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                .contentType("application/json").content("{}"))
            .andExpect(status().isUnauthorized());
        org.junit.jupiter.api.Assertions.assertTrue(session.isInvalid());
        org.mockito.Mockito.verifyNoInteractions(jdbc);
    }

    @Test
    void authenticatedSessionWithoutStartTimeIsRejected() throws Exception {
        var session = authenticatedSession();
        mvc.perform(get("/api/v1/projects").session(session))
            .andExpect(status().isUnauthorized());
        org.junit.jupiter.api.Assertions.assertTrue(session.isInvalid());
    }

    @Test
    void validSessionStillRequiresCsrfForLogout() throws Exception {
        var session = authenticatedSession();
        session.setAttribute(AbsoluteSessionExpiryFilter.SESSION_START,
                java.time.Instant.now().getEpochSecond());
        mvc.perform(post("/api/v1/auth/logout").session(session))
            .andExpect(status().isForbidden());
        org.junit.jupiter.api.Assertions.assertFalse(session.isInvalid());
    }

    private org.springframework.mock.web.MockHttpSession authenticatedSession() {
        var session = new org.springframework.mock.web.MockHttpSession();
        var context = org.springframework.security.core.context.SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                "gestor@example.org", null,
                java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("USER"))));
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        return session;
    }

    @Test
    void anonymousUserCannotAccessProfile() throws Exception {
        mvc.perform(get("/api/v1/users/me"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void anonymousUserCannotAccessProjects() throws Exception {
        mvc.perform(get("/api/v1/projects"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void loginWithoutCsrfIsRejected() throws Exception {
        mvc.perform(post("/api/v1/auth/login")
                .contentType("application/json")
                .content("""
                    {
                        "username": "gestor@example.org",
                        "password": "password"
                    }
                    """))
            .andExpect(status().isForbidden());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({
        "POST,/api/v1/projects", "PATCH,/api/v1/projects/1", "DELETE,/api/v1/projects/1",
        "POST,/api/v1/projects/1/tasks", "PATCH,/api/v1/tasks/1", "DELETE,/api/v1/tasks/1",
        "PATCH,/api/v1/tasks/1/status", "PATCH,/api/v1/tasks/1/priority",
        "PUT,/api/v1/tasks/1/responsibles", "POST,/api/v1/tasks/1/comments", "POST,/api/v1/auth/logout"
    })
    void anonymousWritesReturn401WithOrWithoutCsrf(String method, String path) throws Exception {
        for (boolean token : new boolean[]{false, true}) {
            var request = request(org.springframework.http.HttpMethod.valueOf(method), path)
                .contentType("application/json").content("{}");
            if (token) request.with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf());
            mvc.perform(request).andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.title").value("Unauthorized"))
                .andExpect(jsonPath("$.status").value(401)).andExpect(jsonPath("$.detail").isString());
        }
        org.mockito.Mockito.verifyNoInteractions(jdbc);
    }

    @Test
    void authenticatedInvalidCsrfAndAnonymousLoginStillReturn403() throws Exception {
        var session = authenticatedSession();
        session.setAttribute(AbsoluteSessionExpiryFilter.SESSION_START, java.time.Instant.now().getEpochSecond());
        mvc.perform(post("/api/v1/projects").session(session)
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf().useInvalidToken())
                .contentType("application/json").content("{}"))
            .andExpect(status().isForbidden()).andExpect(content().contentTypeCompatibleWith("application/problem+json"))
            .andExpect(jsonPath("$.status").value(403));
        mvc.perform(post("/api/v1/auth/login").contentType("application/json").content("{}"))
            .andExpect(status().isForbidden()).andExpect(content().contentTypeCompatibleWith("application/problem+json"))
            .andExpect(jsonPath("$.status").value(403));
        org.mockito.Mockito.verifyNoInteractions(jdbc);
    }

    @Test
    void malformedJsonAndValidationHaveDistinctProblemStatuses() throws Exception {
        var session = authenticatedSession();
        session.setAttribute(AbsoluteSessionExpiryFilter.SESSION_START, java.time.Instant.now().getEpochSecond());
        mvc.perform(post("/api/v1/projects").session(session)
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                .contentType("application/json").content("{"))
            .andExpect(status().isBadRequest()).andExpect(content().contentTypeCompatibleWith("application/problem+json"))
            .andExpect(jsonPath("$.status").value(400)).andExpect(jsonPath("$.detail").isString());
        org.mockito.Mockito.when(jdbc.query(org.mockito.ArgumentMatchers.contains("SELECT id FROM app_users"),
                org.mockito.ArgumentMatchers.<org.springframework.jdbc.core.RowMapper<Long>>any(), org.mockito.ArgumentMatchers.anyString()))
            .thenReturn(java.util.List.of(1L));
        mvc.perform(post("/api/v1/projects").session(session)
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                .contentType("application/json").content("{\"name\":\"x\"}"))
            .andExpect(status().isUnprocessableEntity()).andExpect(content().contentTypeCompatibleWith("application/problem+json"))
            .andExpect(jsonPath("$.status").value(422)).andExpect(jsonPath("$.errors[0].field").value("name"))
            .andExpect(jsonPath("$.errors[0].message").isString());
    }

    @Test
    void csrfEndpointIsPublic() throws Exception {
        mvc.perform(get("/api/v1/auth/csrf"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").exists())
            .andExpect(jsonPath("$.headerName").value("X-CSRF-TOKEN"));
    }
}
