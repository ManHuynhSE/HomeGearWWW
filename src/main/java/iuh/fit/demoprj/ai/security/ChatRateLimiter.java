package iuh.fit.demoprj.ai.security;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class ChatRateLimiter {
    private static final int MAX_REQUESTS = 10;
    private static final int MAX_SUBJECTS = 4096;
    private static final long WINDOW_NANOS = TimeUnit.MINUTES.toNanos(1);

    private final LongSupplier nanoTime;
    private final Map<String, ArrayDeque<Long>> requestsBySubject = new LinkedHashMap<>(16, 0.75f, true);

    public ChatRateLimiter() {
        this(System::nanoTime);
    }

    ChatRateLimiter(LongSupplier nanoTime) {
        this.nanoTime = nanoTime;
    }

    public synchronized void check(Authentication authentication, String remoteAddress) {
        String subject = subject(authentication, remoteAddress);
        long now = nanoTime.getAsLong();
        ArrayDeque<Long> requests = requestsBySubject.get(subject);
        if (requests == null) {
            if (requestsBySubject.size() >= MAX_SUBJECTS) {
                Iterator<String> subjects = requestsBySubject.keySet().iterator();
                subjects.next();
                subjects.remove();
            }
            requests = new ArrayDeque<>();
            requestsBySubject.put(subject, requests);
        }

        while (!requests.isEmpty() && now - requests.peekFirst() >= WINDOW_NANOS) {
            requests.removeFirst();
        }
        if (requests.size() >= MAX_REQUESTS) {
            long remainingNanos = WINDOW_NANOS - (now - requests.peekFirst());
            long retryAfterSeconds = Math.max(1,
                    (remainingNanos + TimeUnit.SECONDS.toNanos(1) - 1) / TimeUnit.SECONDS.toNanos(1));
            throw new ChatRateLimitExceededException(retryAfterSeconds);
        }
        requests.addLast(now);
    }

    private static String subject(Authentication authentication, String remoteAddress) {
        if (authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)
                && authentication.getName() != null && !authentication.getName().isBlank()) {
            return "account:" + authentication.getName();
        }
        return "ip:" + (remoteAddress == null || remoteAddress.isBlank() ? "unknown" : remoteAddress);
    }
}
