package iuh.fit.demoprj.ai.service;

import com.google.genai.errors.ApiException;
import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.concurrent.TimeoutException;

public class AiProviderException extends RuntimeException {

    public enum Failure {
        TIMEOUT,
        QUOTA,
        PROVIDER
    }

    private final Failure failure;

    private AiProviderException(Failure failure, Throwable cause) {
        super("AI provider request failed.", cause);
        this.failure = failure;
    }

    public static AiProviderException from(Throwable cause) {
        Failure failure = Failure.PROVIDER;
        for (Throwable current = cause; current != null; current = current.getCause()) {
            if (current instanceof TimeoutException
                    || current instanceof SocketTimeoutException
                    || current instanceof HttpTimeoutException
                    || current instanceof InterruptedIOException) {
                failure = Failure.TIMEOUT;
                break;
            }
            if (current instanceof ApiException apiException) {
                if (apiException.code() == 429) {
                    failure = Failure.QUOTA;
                    break;
                }
                if (apiException.code() == 408 || apiException.code() == 504) {
                    failure = Failure.TIMEOUT;
                    break;
                }
            }
        }
        return new AiProviderException(failure, cause);
    }

    public Failure getFailure() {
        return failure;
    }
}
