package iuh.fit.demoprj.tracking.service;

import iuh.fit.demoprj.repository.ProductRepository;
import iuh.fit.demoprj.tracking.config.TrackingProperties;
import iuh.fit.demoprj.tracking.model.EventType;
import iuh.fit.demoprj.tracking.model.EventSource;
import iuh.fit.demoprj.tracking.model.ProductEvent;
import iuh.fit.demoprj.tracking.repository.ProductEventRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.EnumSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class TrackingService {
    private static final Logger logger = LoggerFactory.getLogger(TrackingService.class);
    private static final EnumSet<EventType> DEDUPED_EVENTS =
            EnumSet.of(EventType.PRODUCT_VIEW, EventType.PRODUCT_CLICK);

    private final ProductEventRepository events;
    private final ProductRepository products;
    private final TrackingProperties properties;
    private final Clock clock;

    public TrackingService(ProductEventRepository events, ProductRepository products,
                           TrackingProperties properties, Clock trackingClock) {
        this.events = events;
        this.products = products;
        this.properties = properties;
        this.clock = trackingClock;
    }

    @Async("trackingExecutor")
    public void record(Long productId, EventType eventType, EventSource source, TrackingContext context) {
        try {
            if (!properties.isEnabled() || context == null || context.isAdmin() || context.isBot()) {
                return;
            }
            if (productId == null || productId <= 0 || eventType == null
                    || context.sessionHash() == null || context.sessionHash().isBlank()) {
                return;
            }
            if (!products.existsByIdAndActiveTrue(productId)) {
                return;
            }

            LocalDateTime now = LocalDateTime.now(clock);
            if (DEDUPED_EVENTS.contains(eventType)) {
                LocalDateTime windowStart = now.minusMinutes(properties.getDedupeWindowMinutes());
                boolean duplicate = context.userId() == null
                        ? events.existsByProductIdAndEventTypeAndSessionHashAndCreatedAtGreaterThanEqual(
                                productId, eventType, context.sessionHash(), windowStart)
                        : events.existsByProductIdAndEventTypeAndUserIdAndCreatedAtGreaterThanEqual(
                                productId, eventType, context.userId(), windowStart);
                if (duplicate) {
                    return;
                }
            }

            events.save(new ProductEvent(productId, eventType, context.userId(), context.sessionHash(), source, now));
        } catch (RuntimeException exception) {
            logger.error("Could not record product tracking event; storefront action will continue.", exception);
        }
    }
}
