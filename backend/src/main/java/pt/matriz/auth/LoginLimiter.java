package pt.matriz.auth;

import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Limite simples para ambiente local; usar armazenamento partilhado em producao. */
@Component
public class LoginLimiter {
    static final int MAX_KEYS = 10_000;
    private static final long WINDOW_SECONDS = 900;
    private record Attempts(int count, long windowStart) {}
    private final Map<String, Attempts> attempts = new HashMap<>();
    private final Clock clock;

    public LoginLimiter() { this(Clock.systemUTC()); }

    LoginLimiter(Clock clock) { this.clock = clock; }

    public synchronized boolean blocked(String key) {
        cleanup(clock.instant().getEpochSecond());
        Attempts a = attempts.get(key);
        // Never evict active counters to admit an untracked key.
        return a == null ? attempts.size() >= MAX_KEYS : a.count() >= 5;
    }

    public synchronized void failure(String key) {
        long now = clock.instant().getEpochSecond();
        cleanup(now);
        Attempts a = attempts.get(key);
        if (a != null) {
            attempts.put(key, new Attempts(Math.min(5, a.count() + 1), a.windowStart()));
        } else if (attempts.size() < MAX_KEYS) {
            attempts.put(key, new Attempts(1, now));
        }
    }

    public synchronized void success(String key) {
        cleanup(clock.instant().getEpochSecond());
        attempts.remove(key);
    }

    private void cleanup(long now) {
        attempts.values().removeIf(a -> now - a.windowStart() >= WINDOW_SECONDS);
    }
}
