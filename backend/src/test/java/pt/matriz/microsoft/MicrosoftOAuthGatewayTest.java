package pt.matriz.microsoft;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class MicrosoftOAuthGatewayTest {
    @Test void libraryBuildsConsumerAuthorizationWithS256AndMinimalScopes() throws Exception {
        var gateway = new MicrosoftOAuthGateway(MicrosoftConnectionServiceTest.settings().registration());
        var request = gateway.authorize();
        assertEquals(MicrosoftConfig.AUTHORIZATION_URI, request.getAuthorizationUri());
        assertEquals(java.util.Set.of("https://graph.microsoft.com/Mail.Send", "offline_access"), request.getScopes());
        String verifier = request.getAttribute("code_verifier");
        assertNotNull(verifier); assertTrue(verifier.length() >= 43);
        assertEquals("S256", request.getAdditionalParameters().get("code_challenge_method"));
        assertEquals(Base64.getUrlEncoder().withoutPadding().encodeToString(
            MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII))),
            request.getAdditionalParameters().get("code_challenge"));
        assertFalse(request.getAuthorizationRequestUri().contains(verifier));
        assertFalse(request.getAuthorizationRequestUri().contains("test-secret"));
        assertNotEquals(request.getState(), gateway.authorize().getState());
        assertEquals("select_account", request.getAdditionalParameters().get("prompt"));
    }

    @Test void librarySendsVerifierAndExactRedirectOnlyToFixedTokenEndpoint() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        var gateway = new MicrosoftOAuthGateway(MicrosoftConnectionServiceTest.settings().registration(), builder);
        var request = gateway.authorize();
        server.expect(requestTo(MicrosoftConfig.TOKEN_URI))
            .andExpect(method(org.springframework.http.HttpMethod.POST))
            .andExpect(content().formDataContains(java.util.Map.of(
                "code_verifier", request.<String>getAttribute("code_verifier"),
                "code", "mock-code", "grant_type", "authorization_code",
                "redirect_uri", MicrosoftConnectionServiceTest.settings().redirectUri(),
                "client_id", "test-client", "client_secret", "test-secret")))
            .andRespond(withSuccess("{\"access_token\":\"mock-access\",\"token_type\":\"Bearer\",\"expires_in\":3600,\"refresh_token\":\"mock-refresh\"}", MediaType.APPLICATION_JSON));
        var result = gateway.exchange(request, "mock-code");
        assertEquals("mock-access", result.getAccessToken().getTokenValue());
        assertEquals("mock-refresh", result.getRefreshToken().getTokenValue()); server.verify();
    }

    @Test void disabledConfigNeedsNoCredentialsAndEnabledFailsClosed() {
        var config = new MicrosoftConfig();
        assertNotNull(config.microsoftOAuthGateway(new MicrosoftConfig.Settings(false, null, null, null)));
        assertThrows(IllegalArgumentException.class,
            () -> config.microsoftOAuthGateway(new MicrosoftConfig.Settings(true, "", "", "")));
        assertFalse(MicrosoftConnectionServiceTest.settings().toString().contains("test-secret"));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {
        "http://evil.example/api/v1/connections/microsoft/callback",
        "https://example.org/other", "https://example.org/api/v1/connections/microsoft/callback?next=evil",
        "https://user@example.org/api/v1/connections/microsoft/callback",
        "https://example.org/api/v1/connections/microsoft/callback#fragment", "not a uri"
    })
    void unsafeConfiguredCallbackIsRejected(String redirect) {
        assertThrows(IllegalArgumentException.class,
            () -> new MicrosoftConfig.Settings(true, "id", "secret", redirect).registration());
    }
}
