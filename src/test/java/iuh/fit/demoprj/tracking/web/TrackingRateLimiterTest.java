package iuh.fit.demoprj.tracking.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import iuh.fit.demoprj.tracking.config.TrackingProperties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class TrackingRateLimiterTest {
    @Test
    void limitsEachSessionAndAllowsAgainAfterOneMinute() {
        TrackingProperties properties = new TrackingProperties();
        properties.setRateLimitPerMinute(2);
        AtomicLong time = new AtomicLong();
        TrackingRateLimiter limiter = new TrackingRateLimiter(properties, time::get);

        assertTrue(limiter.tryAcquire("session-a"));
        assertTrue(limiter.tryAcquire("session-a"));
        assertFalse(limiter.tryAcquire("session-a"));
        assertTrue(limiter.tryAcquire("session-b"));
        time.addAndGet(TimeUnit.MINUTES.toNanos(1));
        assertTrue(limiter.tryAcquire("session-a"));
    }

    @Test
    void rejectsMissingSessionHashes() {
        TrackingRateLimiter limiter = new TrackingRateLimiter(new TrackingProperties(), () -> 0L);

        assertFalse(limiter.tryAcquire(null));
        assertFalse(limiter.tryAcquire(" "));
    }
}
