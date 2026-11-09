package iuh.fit.demoprj.tracking.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import iuh.fit.demoprj.tracking.config.TrackingProperties;
import iuh.fit.demoprj.tracking.model.EventSource;
import iuh.fit.demoprj.tracking.model.EventType;
import iuh.fit.demoprj.tracking.service.TrackingContext;
import iuh.fit.demoprj.tracking.service.TrackingContextFactory;
import iuh.fit.demoprj.tracking.service.TrackingService;
import iuh.fit.demoprj.tracking.web.dto.TrackRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TrackingController {
    private static final Logger logger = LoggerFactory.getLogger(TrackingController.class);
    private static final int MAX_BODY_BYTES = 512;

    private final TrackingProperties properties;
    private final TrackingContextFactory contextFactory;
    private final TrackingRateLimiter rateLimiter;
    private final TrackingService tracking;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TrackingController(TrackingProperties properties, TrackingContextFactory contextFactory,
                              TrackingRateLimiter rateLimiter, TrackingService tracking) {
        this.properties = properties;
        this.contextFactory = contextFactory;
        this.rateLimiter = rateLimiter;
        this.tracking = tracking;
    }

    @PostMapping("/api/track")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void track(HttpServletRequest request, HttpSession session, Authentication authentication) {
        if (!properties.isEnabled() || request.getContentLengthLong() > MAX_BODY_BYTES
                || request.getContentType() == null
                || !request.getContentType().toLowerCase(Locale.ROOT).startsWith("application/json")) {
            return;
        }

        try {
            TrackingContext context = contextFactory.create(
                    authentication, session.getId(), request.getHeader("User-Agent"));
            if (context.isAdmin() || context.isBot() || !rateLimiter.tryAcquire(context.sessionHash())) {
                return;
            }

            byte[] body = request.getInputStream().readNBytes(MAX_BODY_BYTES + 1);
            if (body.length == 0 || body.length > MAX_BODY_BYTES) {
                return;
            }
            TrackRequest trackRequest = objectMapper.readValue(body, TrackRequest.class);
            if (trackRequest.event() == null
                    || !"PRODUCT_CLICK".equals(trackRequest.event().toUpperCase(Locale.ROOT))
                    || trackRequest.productId() == null || trackRequest.productId() <= 0
                    || trackRequest.source() == null) {
                return;
            }

            EventSource source = EventSource.valueOf(trackRequest.source().toUpperCase(Locale.ROOT));
            tracking.record(trackRequest.productId(), EventType.PRODUCT_CLICK, source, context);
        } catch (IOException | IllegalArgumentException exception) {
            logger.debug("Ignoring invalid product tracking request: {}", exception.getMessage());
        }
    }
}
