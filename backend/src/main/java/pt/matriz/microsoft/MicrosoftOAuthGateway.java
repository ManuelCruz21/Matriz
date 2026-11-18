package pt.matriz.microsoft;

import java.time.Duration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.RestClientAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.endpoint.*;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.web.client.RestClient;

/** Library-backed protocol boundary. No discovery, user-info, Graph, refresh or send calls. */
class MicrosoftOAuthGateway {
    private final ClientRegistration registration;
    private final RestClientAuthorizationCodeTokenResponseClient client = new RestClientAuthorizationCodeTokenResponseClient();

    MicrosoftOAuthGateway(ClientRegistration registration) {
        this(registration, restClientBuilder());
    }

    private static RestClient.Builder restClientBuilder() {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(10));
        return RestClient.builder().requestFactory(factory);
    }

    MicrosoftOAuthGateway(ClientRegistration registration, RestClient.Builder builder) {
        this.registration = registration;
        client.setRestClient(builder
            .messageConverters(c -> {
                c.clear();
                c.add(new org.springframework.http.converter.FormHttpMessageConverter());
                c.add(new OAuth2AccessTokenResponseHttpMessageConverter());
            })
            .defaultStatusHandler(new OAuth2ErrorResponseErrorHandler()).build());
    }

    OAuth2AuthorizationRequest authorize() {
        var builder = OAuth2AuthorizationRequest.authorizationCode()
            .authorizationUri(registration.getProviderDetails().getAuthorizationUri())
            .clientId(registration.getClientId()).redirectUri(registration.getRedirectUri())
            .scopes(registration.getScopes());
        // Spring generates a high-entropy verifier and S256 challenge even for this confidential client.
        builder.state(new org.springframework.security.crypto.keygen.Base64StringKeyGenerator(
            java.util.Base64.getUrlEncoder().withoutPadding(), 32).generateKey());
        OAuth2AuthorizationRequestCustomizers.withPkce().accept(builder);
        builder.additionalParameters(p -> p.put("prompt", "select_account"));
        return builder.build();
    }

    OAuth2AccessTokenResponse exchange(OAuth2AuthorizationRequest request, String code) {
        var response = OAuth2AuthorizationResponse.success(code).state(request.getState())
            .redirectUri(registration.getRedirectUri()).build();
        return client.getTokenResponse(new OAuth2AuthorizationCodeGrantRequest(registration,
            new OAuth2AuthorizationExchange(request, response)));
    }
}
