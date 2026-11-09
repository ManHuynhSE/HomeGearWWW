package iuh.fit.demoprj.tracking.web;

import iuh.fit.demoprj.tracking.config.TrackingProperties;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.LongSupplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TrackingRateLimiter {
    private static final int MAX_SESSIONS = 10_000;
    private static final long WINDOW_NANOS = 60_000_000_000L;

    private final TrackingProperties properties;
    private final LongSupplier nanoTime;
    private final Map<String, ArrayDeque<Long>> eventsBySession = new LinkedHashMap<>(16, 0.75f, true);

    @Autowired
    public TrackingRateLimiter(TrackingProperties properties) {
        this(properties, System::nanoTime);
    }

    TrackingRateLimiter(TrackingProperties properties, LongSupplier nanoTime) {
        this.properties = properties;
        this.nanoTime = nanoTime;
    }

    public synchronized boolean tryAcquire(String sessionHash) {
        if (sessionHash == null || sessionHash.isBlank()) {
            return false;
        }
        long now = nanoTime.getAsLong();
        ArrayDeque<Long> timestamps = eventsBySession.get(sessionHash);
        if (timestamps == null) {
            if (eventsBySession.size() >= MAX_SESSIONS) {
                Iterator<String> oldestSessions = eventsBySession.keySet().iterator();
                oldestSessions.next();
                oldestSessions.remove();
            }
            timestamps = new ArrayDeque<>();
            eventsBySession.put(sessionHash, timestamps);
        }

        while (!timestamps.isEmpty() && now - timestamps.peekFirst() >= WINDOW_NANOS) {
            timestamps.removeFirst();
        }
        if (timestamps.size() >= properties.getRateLimitPerMinute()) {
            return false;
        }
        timestamps.addLast(now);
        return true;
    }
}
