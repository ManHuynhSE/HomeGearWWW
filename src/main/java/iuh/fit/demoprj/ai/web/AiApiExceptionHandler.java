package iuh.fit.demoprj.ai.web;

import iuh.fit.demoprj.ai.service.AiProviderException;
import iuh.fit.demoprj.ai.service.AiServiceUnavailableException;
import iuh.fit.demoprj.ai.security.ChatRateLimitExceededException;
import iuh.fit.demoprj.ai.web.dto.ChatErrorResponse;
import jakarta.validation.ConstraintViolationException;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "iuh.fit.demoprj.ai.web")
public class AiApiExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(AiApiExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ChatErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> Objects.requireNonNullElse(error.getDefaultMessage(), "Yêu cầu không hợp lệ."))
                .orElse("Yêu cầu không hợp lệ.");
        return ResponseEntity.badRequest().body(new ChatErrorResponse(message));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, ConstraintViolationException.class})
    ResponseEntity<ChatErrorResponse> handleInvalidRequest(Exception exception) {
        return ResponseEntity.badRequest().body(new ChatErrorResponse("Dữ liệu gửi lên không hợp lệ."));
    }

    @ExceptionHandler(AiServiceUnavailableException.class)
    ResponseEntity<ChatErrorResponse> handleDisabledAi() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ChatErrorResponse("Dịch vụ AI hiện chưa được bật."));
    }

    @ExceptionHandler(ChatRateLimitExceededException.class)
    ResponseEntity<ChatErrorResponse> handleRateLimit(ChatRateLimitExceededException exception) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, Long.toString(exception.getRetryAfterSeconds()))
                .body(new ChatErrorResponse("Bạn gửi yêu cầu quá nhanh. Vui lòng thử lại sau."));
    }

    @ExceptionHandler(AiProviderException.class)
    ResponseEntity<ChatErrorResponse> handleProviderFailure(AiProviderException exception) {
        return switch (exception.getFailure()) {
            case TIMEOUT -> ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
                    .body(new ChatErrorResponse("AI phản hồi quá lâu. Vui lòng thử lại sau."));
            case QUOTA -> ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new ChatErrorResponse("Dịch vụ AI đang quá tải. Vui lòng thử lại sau."));
            case PROVIDER -> ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(new ChatErrorResponse("Dịch vụ AI đang gặp sự cố. Vui lòng thử lại sau."));
        };
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ChatErrorResponse> handleUnexpectedFailure(Exception exception) {
        logger.error("Unexpected error while handling an AI API request.", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ChatErrorResponse("Không thể xử lý yêu cầu lúc này. Vui lòng thử lại sau."));
    }
}
