package iuh.fit.demoprj.ai.security;

public class ChatRateLimitExceededException extends RuntimeException {
    private final long retryAfterSeconds;

    public ChatRateLimitExceededException(long retryAfterSeconds) {
        super("Chat request limit exceeded.");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
