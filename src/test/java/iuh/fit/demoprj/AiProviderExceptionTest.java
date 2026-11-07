package iuh.fit.demoprj;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.genai.errors.ApiException;
import iuh.fit.demoprj.ai.service.AiProviderException;
import java.net.SocketTimeoutException;
import org.junit.jupiter.api.Test;

class AiProviderExceptionTest {

    @Test
    void classifiesQuotaErrorsWithoutExposingProviderDetails() {
        AiProviderException exception = AiProviderException.from(
                new ApiException(429, "RESOURCE_EXHAUSTED", "private provider detail"));

        assertEquals(AiProviderException.Failure.QUOTA, exception.getFailure());
        org.junit.jupiter.api.Assertions.assertFalse(exception.getMessage().contains("private provider detail"));
    }

    @Test
    void classifiesTimeouts() {
        AiProviderException exception = AiProviderException.from(new SocketTimeoutException("timed out"));

        assertEquals(AiProviderException.Failure.TIMEOUT, exception.getFailure());
    }
}
