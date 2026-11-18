package pt.matriz.microsoft;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.web.server.ResponseStatusException;

/** Single-node, bounded, volatile storage. Never expose requests/verifiers/tokens as response objects. */
public class MicrosoftConnectionService {
    private static final Duration STATE_TTL = Duration.ofMinutes(5);
    private static final int CAPACITY = 1000;
    private final boolean enabled;
    private final MicrosoftOAuthGateway gateway;
    private final Clock clock;
    private final Map<Long, Pending> pending = new HashMap<>();
    private final Map<Long, OAuth2AccessTokenResponse> tokens = new HashMap<>();

    private static final class Pending {
        final String sessionId;
        final OAuth2AuthorizationRequest request;
        final Instant expires;
        boolean exchanging;
        Pending(String sessionId, OAuth2AuthorizationRequest request, Instant expires) {
            this.sessionId = sessionId; this.request = request; this.expires = expires;
        }
    }

    public record Status(boolean enabled, boolean connected) {}

    MicrosoftConnectionService(boolean enabled, MicrosoftOAuthGateway gateway, Clock clock) {
        this.enabled = enabled; this.gateway = gateway; this.clock = clock;
    }

    public synchronized Status status(long user) {
        cleanup();
        return new Status(enabled, enabled && tokens.containsKey(user));
    }

    public synchronized String connect(long user, String sessionId) {
        requireEnabled();
        cleanup();
        // An account cannot be silently replaced, including from another Matriz session.
        if (tokens.containsKey(user)) throw failure(HttpStatus.CONFLICT);
        if (!pending.containsKey(user) && pending.size() >= CAPACITY) throw failure(HttpStatus.SERVICE_UNAVAILABLE);
        var request = gateway.authorize();
        pending.put(user, new Pending(sessionId, request, clock.instant().plus(STATE_TTL)));
        return request.getAuthorizationRequestUri();
    }

    public void callback(long user, String sessionId, String state, String code, String error,
            BooleanSupplier sessionStillValid) {
        final Pending attempt;
        synchronized (this) {
            requireEnabled();
            cleanup();
            attempt = pending.get(user);
            if (attempt == null || attempt.exchanging || state == null || !attempt.sessionId.equals(sessionId)
                    || !java.security.MessageDigest.isEqual(attempt.request.getState().getBytes(java.nio.charset.StandardCharsets.UTF_8),
                        state.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                throw failure(HttpStatus.BAD_REQUEST);
            // Consume before any exchange, including provider denial/failure. No retry/replay.
            attempt.exchanging = true;
            if (error != null || code == null || code.isBlank() || code.length() > 8192) {
                pending.remove(user, attempt);
                throw failure(HttpStatus.BAD_REQUEST);
            }
            if (tokens.size() >= CAPACITY) {
                pending.remove(user, attempt);
                throw failure(HttpStatus.SERVICE_UNAVAILABLE);
            }
        }

        // Never hold the shared monitor across provider I/O.
        final OAuth2AccessTokenResponse result;
        try {
            result = gateway.exchange(attempt.request, code);
            if (result == null || result.getAccessToken() == null
                    || result.getAccessToken().getExpiresAt() == null
                    || !clock.instant().isBefore(result.getAccessToken().getExpiresAt()))
                throw new IllegalStateException();
        } catch (RuntimeException ex) {
            synchronized (this) {
                pending.remove(user, attempt);
            }
            // Provider exceptions can contain sensitive response data; never propagate/log them.
            throw failure(HttpStatus.BAD_GATEWAY);
        }

        synchronized (this) {
            cleanup();
            // Disconnect, expiry or a newer connect cancels this exchange, never its successor.
            if (pending.get(user) != attempt) throw failure(HttpStatus.BAD_REQUEST);
            pending.remove(user, attempt);
            if (!sessionStillValid.getAsBoolean()) throw failure(HttpStatus.UNAUTHORIZED);
            // Other exchanges may have completed while this one was outside the monitor.
            if (tokens.size() >= CAPACITY) throw failure(HttpStatus.SERVICE_UNAVAILABLE);
            tokens.put(user, result);
        }
    }

    public synchronized void disconnect(long user) {
        requireEnabled();
        pending.remove(user);
        tokens.remove(user);
    }

    private void cleanup() {
        Instant now = clock.instant();
        pending.values().removeIf(p -> !now.isBefore(p.expires));
        tokens.values().removeIf(t -> !now.isBefore(t.getAccessToken().getExpiresAt()));
    }
    private void requireEnabled() { if (!enabled) throw failure(HttpStatus.SERVICE_UNAVAILABLE); }
    private static ResponseStatusException failure(HttpStatus status) {
        return new ResponseStatusException(status, "Microsoft connection request could not be completed");
    }
}
