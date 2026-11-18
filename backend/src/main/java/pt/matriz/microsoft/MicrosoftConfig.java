package pt.matriz.microsoft;

import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

@Configuration
@EnableConfigurationProperties(MicrosoftConfig.Settings.class)
public class MicrosoftConfig {
    static final String AUTHORIZATION_URI = "https://login.microsoftonline.com/consumers/oauth2/v2.0/authorize";
    static final String TOKEN_URI = "https://login.microsoftonline.com/consumers/oauth2/v2.0/token";

    @ConfigurationProperties("matriz.microsoft")
    public record Settings(boolean enabled, String clientId, String clientSecret, String redirectUri) {
        ClientRegistration registration() {
            if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank())
                throw new IllegalArgumentException("Microsoft client configuration is required when enabled");
            URI uri;
            try { uri = URI.create(redirectUri); }
            catch (RuntimeException ex) { throw new IllegalArgumentException("Invalid Microsoft callback configuration"); }
            boolean local = "http".equals(uri.getScheme()) && "localhost".equals(uri.getHost());
            if ((!"https".equals(uri.getScheme()) && !local) || uri.getHost() == null
                    || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null
                    || !"/api/v1/connections/microsoft/callback".equals(uri.getPath()))
                throw new IllegalArgumentException("Invalid Microsoft callback configuration");
            return ClientRegistration.withRegistrationId("microsoft-connection")
                .clientId(clientId).clientSecret(clientSecret)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE).redirectUri(redirectUri)
                .authorizationUri(AUTHORIZATION_URI).tokenUri(TOKEN_URI)
                .scope("https://graph.microsoft.com/Mail.Send", "offline_access").build();
        }
        // Do not let record-generated toString expose the secret.
        @Override public String toString() { return "MicrosoftSettings[enabled=" + enabled + "]"; }
    }

    @Bean MicrosoftOAuthGateway microsoftOAuthGateway(Settings settings) {
        return new MicrosoftOAuthGateway(settings.enabled() ? settings.registration() : null);
    }

    @Bean MicrosoftConnectionService microsoftConnectionService(Settings settings, MicrosoftOAuthGateway gateway) {
        return new MicrosoftConnectionService(settings.enabled(), gateway, java.time.Clock.systemUTC());
    }
}
