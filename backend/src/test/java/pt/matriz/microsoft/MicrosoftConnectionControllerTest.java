package pt.matriz.microsoft;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pt.matriz.auth.SecurityConfig;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MicrosoftConnectionController.class)
@Import(SecurityConfig.class)
class MicrosoftConnectionControllerTest {
    private static final String BASE = "/api/v1/connections/microsoft";
    @Autowired MockMvc mvc;
    @MockitoBean MicrosoftConnectionService service;
    @MockitoBean JdbcTemplate jdbc;

    private MockHttpSession session(String user) {
        var session = new MockHttpSession();
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(user, null,
            java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("USER"))));
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        session.setAttribute("MATRIZ_SESSION_START", Instant.now().getEpochSecond());
        when(jdbc.queryForObject(anyString(), eq(Long.class), eq(user))).thenReturn(user.equals("one") ? 1L : 2L);
        return session;
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"GET,", "GET,/callback", "POST,/connect", "DELETE,/disconnect", "POST,/disconnect"})
    void anonymousRequestsAreUnauthorized(String method, String suffix) throws Exception {
        for (boolean token : new boolean[]{false, true}) {
            var req = request(org.springframework.http.HttpMethod.valueOf(method), BASE + (suffix == null ? "" : suffix));
            if (token) req.with(csrf());
            mvc.perform(req).andExpect(status().isUnauthorized());
        }
        verifyNoInteractions(service, jdbc);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"POST,/connect", "DELETE,/disconnect", "POST,/disconnect"})
    void writesRequireValidCsrf(String method, String suffix) throws Exception {
        var session = session("one");
        mvc.perform(request(org.springframework.http.HttpMethod.valueOf(method), BASE + suffix).session(session))
            .andExpect(status().isForbidden());
        mvc.perform(request(org.springframework.http.HttpMethod.valueOf(method), BASE + suffix).session(session).with(csrf().useInvalidToken()))
            .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test void connectUsesSessionAndServerResolvedUserNotCallerDestinationOrUser() throws Exception {
        var session = session("one");
        when(service.connect(1, session.getId())).thenReturn("https://login.microsoftonline.com/consumers/mock");
        mvc.perform(post(BASE + "/connect").session(session).with(csrf())
                .param("userId", "2").param("redirect_uri", "https://evil.example"))
            .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
            .andExpect(jsonPath("$.authorizationUrl").value("https://login.microsoftonline.com/consumers/mock"));
        verify(service).connect(1, session.getId());
    }

    @Test void callbackKeepsMatrizAuthenticationAndReturnsNoTokensOrRedirect() throws Exception {
        var session = session("one");
        mvc.perform(get(BASE + "/callback").session(session).param("state", "state").param("code", "code"))
            .andExpect(status().isNoContent()).andExpect(content().string(""))
            .andExpect(header().doesNotExist("Location")).andExpect(header().string("Cache-Control", "no-store"))
            .andExpect(header().string("Referrer-Policy", "no-referrer"));
        verify(service).callback(eq(1L), eq(session.getId()), eq("state"), eq("code"), isNull(), any());
        org.junit.jupiter.api.Assertions.assertEquals("one", ((org.springframework.security.core.context.SecurityContext)
            session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY)).getAuthentication().getName());
    }

    @Test void statusAndDisconnectAreOwnedByAuthenticatedUser() throws Exception {
        when(service.status(2)).thenReturn(new MicrosoftConnectionService.Status(true, false));
        var session = session("two");
        mvc.perform(get(BASE).session(session).param("userId", "1"))
            .andExpect(status().isOk()).andExpect(content().json("{\"enabled\":true,\"connected\":false}", true))
            .andExpect(header().string("Cache-Control", "no-store"));
        mvc.perform(delete(BASE + "/disconnect").session(session).with(csrf()).param("userId", "1"))
            .andExpect(status().isNoContent());
        mvc.perform(post(BASE + "/disconnect").session(session).with(csrf()))
            .andExpect(status().isNoContent());
        verify(service, times(2)).disconnect(2); verify(service, never()).disconnect(1);
    }

    @Test void expiredMatrizSessionCannotCompleteCallback() throws Exception {
        var session = session("one");
        session.setAttribute("MATRIZ_SESSION_START", Instant.now().minusSeconds(28800).getEpochSecond());
        mvc.perform(get(BASE + "/callback").session(session).param("state", "state").param("code", "code"))
            .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test void providerFailureUsesGenericProblemWithoutProviderData() throws Exception {
        var session = session("one");
        doThrow(new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_GATEWAY,
            "Microsoft connection request could not be completed"))
            .when(service).callback(eq(1L), eq(session.getId()), eq("state"), eq("secret-code"), isNull(), any());
        mvc.perform(get(BASE + "/callback").session(session).param("state", "state").param("code", "secret-code"))
            .andExpect(status().isBadGateway()).andExpect(header().string("Cache-Control", "no-store"))
            .andExpect(header().string("Referrer-Policy", "no-referrer"))
            .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("secret-code"))));
    }

    @Test void callbackSessionGuardRejectsSessionInvalidatedDuringExchange() throws Exception {
        var session = session("one");
        doAnswer(invocation -> {
            java.util.function.BooleanSupplier valid = invocation.getArgument(5);
            org.junit.jupiter.api.Assertions.assertTrue(valid.getAsBoolean());
            session.invalidate();
            org.junit.jupiter.api.Assertions.assertFalse(valid.getAsBoolean());
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED);
        }).when(service).callback(eq(1L), eq(session.getId()), eq("state"), eq("code"), isNull(), any());
        mvc.perform(get(BASE + "/callback").session(session).param("state", "state").param("code", "code"))
            .andExpect(status().isUnauthorized());
    }
}
