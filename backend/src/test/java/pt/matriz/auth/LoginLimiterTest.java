package pt.matriz.auth;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class LoginLimiterTest {
    private final MutableClock clock = new MutableClock();
    private final LoginLimiter limiter = new LoginLimiter(clock);

    @Test
    void blocksAfterFiveFailuresUntilExactlyNineHundredSecondsFromFirstFailure() {
        for (int i = 0; i < 4; i++) {
            limiter.failure("user");
            assertFalse(limiter.blocked("user"));
        }
        clock.advance(899);
        limiter.failure("user");
        assertTrue(limiter.blocked("user"));
        limiter.failure("user");
        assertTrue(limiter.blocked("user"));
        clock.advance(1);
        assertFalse(limiter.blocked("user"));
        assertEquals(0, storedKeys());
        limiter.failure("user");
        assertFalse(limiter.blocked("user"));
    }

    @Test
    void successClearsFailures() {
        for (int i = 0; i < 5; i++) limiter.failure("user");
        assertTrue(limiter.blocked("user"));
        limiter.success("user");
        assertFalse(limiter.blocked("user"));
        assertEquals(0, storedKeys());
    }

    @Test
    void cleansExpiredUnrelatedKeysOnFailureAndSuccess() {
        limiter.failure("old");
        clock.advance(900);
        limiter.failure("new");
        assertEquals(1, storedKeys());
        clock.advance(900);
        limiter.success("unrelated");
        assertEquals(0, storedKeys());
    }

    @Test
    void capacityFailsClosedWithoutEvictingLiveCountersAndRecoversAfterExpiry() {
        limiter.failure("old");
        clock.advance(1);
        for (int i = 1; i < LoginLimiter.MAX_KEYS; i++) {
            limiter.failure("user-" + i);
        }
        assertEquals(LoginLimiter.MAX_KEYS, storedKeys());
        assertTrue(limiter.blocked("overflow"));
        limiter.failure("overflow");
        assertEquals(LoginLimiter.MAX_KEYS, storedKeys());
        assertFalse(limiter.blocked("user-1"));
        for (int i = 0; i < 4; i++) limiter.failure("user-1");
        assertTrue(limiter.blocked("user-1"));

        clock.advance(899);
        assertFalse(limiter.blocked("overflow"));
        assertEquals(LoginLimiter.MAX_KEYS - 1, storedKeys());
        assertTrue(limiter.blocked("user-1"));
        limiter.failure("overflow");
        assertEquals(LoginLimiter.MAX_KEYS, storedKeys());
        assertTrue(limiter.blocked("another"));
        limiter.success("overflow");
        assertFalse(limiter.blocked("another"));

        clock.advance(1);
        assertFalse(limiter.blocked("user-1"));
        assertEquals(0, storedKeys());
    }

    private int storedKeys() {
        return ((Map<?, ?>) ReflectionTestUtils.getField(limiter, "attempts")).size();
    }

    private static class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-11-17T12:00:00Z");

        void advance(long seconds) { now = now.plusSeconds(seconds); }

        @Override
        public ZoneId getZone() { return ZoneOffset.UTC; }

        @Override
        public Clock withZone(ZoneId zone) { return Clock.fixed(now, zone); }

        @Override
        public Instant instant() { return now; }
    }
}
