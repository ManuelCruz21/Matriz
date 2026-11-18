package pt.matriz.microsoft;

import java.time.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MicrosoftConnectionServiceTest {
    private final Clock clock = mock(Clock.class);
    private final MicrosoftOAuthGateway gateway = mock(MicrosoftOAuthGateway.class);
    private MicrosoftConnectionService service;
    private OAuth2AuthorizationRequest request;
    private final Instant now = Instant.parse("2026-01-01T00:00:00Z");

    @BeforeEach void setup() {
        when(clock.instant()).thenReturn(now);
        service = new MicrosoftConnectionService(true, gateway, clock);
        request = new MicrosoftOAuthGateway(settings().registration()).authorize();
        when(gateway.authorize()).thenReturn(request);
        when(gateway.exchange(any(), anyString())).thenReturn(OAuth2AccessTokenResponse.withToken("ACCESS-SECRET")
            .tokenType(OAuth2AccessToken.TokenType.BEARER).expiresIn(3600).refreshToken("REFRESH-SECRET").build());
    }

    static MicrosoftConfig.Settings settings() {
        return new MicrosoftConfig.Settings(true, "test-client", "test-secret",
            "http://localhost:8080/api/v1/connections/microsoft/callback");
    }
    private void begin() { service.connect(1, "session"); }
    private void finish() { service.callback(1, "session", request.getState(), "code", null, () -> true); }
    private void fails(int status, org.junit.jupiter.api.function.Executable action) {
        assertEquals(status, assertThrows(ResponseStatusException.class, action).getStatusCode().value());
    }

    @Test void disabledDoesNotTouchProvider() {
        service = new MicrosoftConnectionService(false, gateway, clock);
        assertEquals(new MicrosoftConnectionService.Status(false, false), service.status(1));
        fails(503, this::begin); fails(503, this::finish); fails(503, () -> service.disconnect(1));
        verifyNoInteractions(gateway);
    }
    @Test void connectedStatusHasOnlyBooleansAndOwnershipIsIsolated() throws Exception {
        begin(); finish();
        assertTrue(service.status(1).connected()); assertFalse(service.status(2).connected());
        assertEquals("{\"enabled\":true,\"connected\":true}",
            new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(service.status(1)));
        service.disconnect(2); assertTrue(service.status(1).connected());
        service.disconnect(1); assertFalse(service.status(1).connected());
    }
    @Test void stateIsUserAndSessionBound() {
        begin();
        fails(400, () -> service.callback(2, "session", request.getState(), "code", null, () -> true));
        fails(400, () -> service.callback(1, "other-session", request.getState(), "code", null, () -> true));
        fails(400, () -> service.callback(1, "session", "wrong", "code", null, () -> true));
        fails(400, () -> service.callback(1, "session", null, "code", null, () -> true));
        verify(gateway, never()).exchange(any(), any());
        finish(); assertTrue(service.status(1).connected());
    }
    @Test void replayIsRejected() {
        begin(); finish(); fails(400, this::finish);
        verify(gateway, times(1)).exchange(any(), any());
    }
    @Test void stateExpiresAtFiveMinutes() {
        begin(); when(clock.instant()).thenReturn(now.plusSeconds(300));
        fails(400, this::finish); verify(gateway, never()).exchange(any(), any());
    }
    @Test void denialConsumesStateWithoutExchange() {
        begin(); fails(400, () -> service.callback(1, "session", request.getState(), null, "access_denied", () -> true));
        fails(400, this::finish); verify(gateway, never()).exchange(any(), any());
    }
    @Test void missingCodeConsumesState() {
        begin(); fails(400, () -> service.callback(1, "session", request.getState(), "", null, () -> true));
        fails(400, this::finish); verify(gateway, never()).exchange(any(), any());
    }
    @Test void exchangeFailureIsSanitizedAndSingleUse() {
        when(gateway.exchange(any(), any())).thenThrow(new IllegalStateException("PROVIDER-SECRET"));
        begin();
        var ex = assertThrows(ResponseStatusException.class, this::finish);
        assertEquals(502, ex.getStatusCode().value()); assertFalse(ex.toString().contains("PROVIDER-SECRET"));
        assertNull(ex.getCause()); fails(400, this::finish); assertFalse(service.status(1).connected());
    }
    @Test void cannotReplaceConnectedMicrosoftAccountWithoutDisconnect() {
        begin(); finish(); fails(409, this::begin);
        service.disconnect(1); begin(); finish(); assertTrue(service.status(1).connected());
    }
    @Test void disconnectCancelsPendingAuthorization() {
        begin(); service.disconnect(1); fails(400, this::finish);
        verify(gateway, never()).exchange(any(), any());
    }
    @Test void newInitiationInvalidatesOldState() {
        begin(); var next = new MicrosoftOAuthGateway(settings().registration()).authorize();
        when(gateway.authorize()).thenReturn(next); begin(); fails(400, this::finish);
        service.callback(1, "session", next.getState(), "new-code", null, () -> true);
        assertTrue(service.status(1).connected());
    }
    @Test void expiredAccessTokensAreEvictedIncludingRefreshToken() {
        begin(); finish(); when(clock.instant()).thenReturn(Instant.now().plusSeconds(7200));
        assertFalse(service.status(1).connected());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"disconnect", "reconnect", "reconnect-pending", "logout", "provider-failure"})
    void inFlightExchangeCannotPublishAfterCancellation(String cancellation) throws Exception {
        var entered = new java.util.concurrent.CountDownLatch(1);
        var release = new java.util.concurrent.CountDownLatch(1);
        var valid = new java.util.concurrent.atomic.AtomicBoolean(true);
        var result = OAuth2AccessTokenResponse.withToken("old-token")
            .tokenType(OAuth2AccessToken.TokenType.BEARER).expiresIn(3600).build();
        when(gateway.exchange(any(), eq("code"))).thenAnswer(invocation -> {
            entered.countDown();
            assertTrue(release.await(5, java.util.concurrent.TimeUnit.SECONDS));
            if (cancellation.equals("provider-failure")) throw new IllegalStateException("PROVIDER-SECRET");
            return result;
        });
        begin();
        try (var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            var callback = executor.submit(() -> assertThrows(ResponseStatusException.class,
                () -> service.callback(1, "session", request.getState(), "code", null, valid::get)));
            try {
                assertTrue(entered.await(5, java.util.concurrent.TimeUnit.SECONDS));
                // These operations must complete BEFORE the blocked provider is released.
                executor.submit(() -> {
                    assertFalse(service.status(2).connected());
                    fails(400, this::finish); // In-flight state is already consumed.
                    if (cancellation.equals("logout")) valid.set(false);
                    else if (cancellation.equals("disconnect")) service.disconnect(1);
                    else {
                        service.connect(1, "new-session");
                        if (!cancellation.equals("reconnect-pending"))
                            service.callback(1, "new-session", request.getState(), "new-code", null, () -> true);
                    }
                }).get(2, java.util.concurrent.TimeUnit.SECONDS);
            } finally {
                release.countDown();
            }
            var failure = callback.get(5, java.util.concurrent.TimeUnit.SECONDS);
            assertEquals(cancellation.equals("logout") ? 401 : cancellation.equals("provider-failure") ? 502 : 400,
                failure.getStatusCode().value());
            assertEquals(cancellation.equals("reconnect") || cancellation.equals("provider-failure"),
                service.status(1).connected());
            if (cancellation.equals("reconnect-pending")) {
                service.callback(1, "new-session", request.getState(), "new-code", null, () -> true);
                assertTrue(service.status(1).connected());
            }
        }
    }
}
