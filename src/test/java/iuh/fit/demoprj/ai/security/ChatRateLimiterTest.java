package iuh.fit.demoprj.ai.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class ChatRateLimiterTest {
    @Test
    void limitsEachAuthenticatedAccountToTenRequestsPerMinute() {
        AtomicLong time = new AtomicLong();
        ChatRateLimiter limiter = new ChatRateLimiter(time::get);
        var alice = new UsernamePasswordAuthenticationToken(
                "alice@example.com", "password", List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
        var bob = new UsernamePasswordAuthenticationToken(
                "bob@example.com", "password", List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));

        for (int request = 0; request < 10; request++) {
            limiter.check(alice, "127.0.0.1");
        }
        ChatRateLimitExceededException exception = assertThrows(
                ChatRateLimitExceededException.class, () -> limiter.check(alice, "127.0.0.1"));

        assertEquals(60, exception.getRetryAfterSeconds());
        assertDoesNotThrow(() -> limiter.check(bob, "127.0.0.1"));
        time.addAndGet(TimeUnit.MINUTES.toNanos(1));
        assertDoesNotThrow(() -> limiter.check(alice, "127.0.0.1"));
    }

    @Test
    void guestsAreLimitedByRemoteAddress() {
        ChatRateLimiter limiter = new ChatRateLimiter(() -> 0L);
        for (int request = 0; request < 10; request++) {
            limiter.check(null, "192.0.2.1");
        }

        assertThrows(ChatRateLimitExceededException.class, () -> limiter.check(null, "192.0.2.1"));
        assertDoesNotThrow(() -> limiter.check(null, "192.0.2.2"));
    }
}
