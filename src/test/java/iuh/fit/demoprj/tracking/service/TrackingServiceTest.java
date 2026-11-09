package iuh.fit.demoprj.tracking.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import iuh.fit.demoprj.repository.ProductRepository;
import iuh.fit.demoprj.tracking.config.TrackingProperties;
import iuh.fit.demoprj.tracking.model.EventSource;
import iuh.fit.demoprj.tracking.model.EventType;
import iuh.fit.demoprj.tracking.model.ProductEvent;
import iuh.fit.demoprj.tracking.repository.ProductEventRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TrackingServiceTest {
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-11-09T04:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));

    @Mock
    private ProductEventRepository events;
    @Mock
    private ProductRepository products;

    private TrackingProperties properties;
    private TrackingService tracking;

    @BeforeEach
    void setUp() {
        properties = new TrackingProperties();
        tracking = new TrackingService(events, products, properties, CLOCK);
    }

    @Test
    void skipsWhenDisabledOrContextIsExcluded() {
        TrackingContext regular = new TrackingContext(7L, "a".repeat(64), false, false);

        properties.setEnabled(false);
        tracking.record(12L, EventType.PRODUCT_VIEW, EventSource.DETAIL, regular);
        properties.setEnabled(true);
        tracking.record(12L, EventType.PRODUCT_VIEW, EventSource.DETAIL,
                new TrackingContext(null, "a".repeat(64), false, true));
        tracking.record(12L, EventType.PRODUCT_VIEW, EventSource.DETAIL,
                new TrackingContext(null, "a".repeat(64), true, false));

        verifyNoInteractions(products, events);
    }

    @Test
    void skipsMissingInactiveOrInvalidProductAndInvalidContext() {
        when(products.existsByIdAndActiveTrue(12L)).thenReturn(false);
        tracking.record(12L, EventType.PRODUCT_VIEW, EventSource.DETAIL,
                new TrackingContext(null, "a".repeat(64), false, false));
        tracking.record(0L, EventType.PRODUCT_VIEW, EventSource.DETAIL,
                new TrackingContext(null, "a".repeat(64), false, false));
        tracking.record(12L, EventType.PRODUCT_VIEW, EventSource.DETAIL,
                new TrackingContext(null, "", false, false));

        verify(products).existsByIdAndActiveTrue(12L);
        verifyNoInteractions(events);
    }

    @Test
    void deduplicatesViewsAndClicksUsingAccountIdentityWithinConfiguredWindow() {
        when(products.existsByIdAndActiveTrue(12L)).thenReturn(true);
        when(events.existsByProductIdAndEventTypeAndUserIdAndCreatedAtGreaterThanEqual(
                12L, EventType.PRODUCT_VIEW, 7L, java.time.LocalDateTime.of(2026, 11, 9, 10, 30)))
                .thenReturn(true);
        when(events.existsByProductIdAndEventTypeAndUserIdAndCreatedAtGreaterThanEqual(
                12L, EventType.PRODUCT_CLICK, 7L, java.time.LocalDateTime.of(2026, 11, 9, 10, 30)))
                .thenReturn(false);
        TrackingContext customer = new TrackingContext(7L, "a".repeat(64), false, false);

        tracking.record(12L, EventType.PRODUCT_VIEW, EventSource.DETAIL, customer);
        tracking.record(12L, EventType.PRODUCT_CLICK, EventSource.LISTING, customer);

        ArgumentCaptor<ProductEvent> event = ArgumentCaptor.forClass(ProductEvent.class);
        verify(events).save(event.capture());
        org.junit.jupiter.api.Assertions.assertEquals(EventType.PRODUCT_CLICK, event.getValue().getEventType());
        org.junit.jupiter.api.Assertions.assertEquals(7L, event.getValue().getUserId());
        org.junit.jupiter.api.Assertions.assertEquals(
                java.time.LocalDateTime.of(2026, 11, 9, 11, 0), event.getValue().getCreatedAt());
    }

    @Test
    void recordsGuestEventWhenRepositoryHasNoRecentSessionMatch() {
        when(products.existsByIdAndActiveTrue(12L)).thenReturn(true);
        when(events.existsByProductIdAndEventTypeAndSessionHashAndCreatedAtGreaterThanEqual(
                12L, EventType.PRODUCT_VIEW, "a".repeat(64), java.time.LocalDateTime.of(2026, 11, 9, 10, 30)))
                .thenReturn(false);

        tracking.record(12L, EventType.PRODUCT_VIEW, EventSource.DETAIL,
                new TrackingContext(null, "a".repeat(64), false, false));

        verify(events).save(any(ProductEvent.class));
    }

    @Test
    void recordsAgainAfterDeduplicationWindowExpires() {
        when(products.existsByIdAndActiveTrue(12L)).thenReturn(true);
        when(events.existsByProductIdAndEventTypeAndSessionHashAndCreatedAtGreaterThanEqual(
                12L, EventType.PRODUCT_VIEW, "a".repeat(64), java.time.LocalDateTime.of(2026, 11, 9, 11, 1)))
                .thenReturn(false);
        TrackingService afterWindow = new TrackingService(
                events, products, properties, Clock.offset(CLOCK, java.time.Duration.ofMinutes(31)));

        afterWindow.record(12L, EventType.PRODUCT_VIEW, EventSource.DETAIL,
                new TrackingContext(null, "a".repeat(64), false, false));

        verify(events).save(any(ProductEvent.class));
    }

    @Test
    void deduplicatesGuestViewsBySessionHash() {
        when(products.existsByIdAndActiveTrue(12L)).thenReturn(true);
        when(events.existsByProductIdAndEventTypeAndSessionHashAndCreatedAtGreaterThanEqual(
                12L, EventType.PRODUCT_VIEW, "a".repeat(64), java.time.LocalDateTime.of(2026, 11, 9, 10, 30)))
                .thenReturn(true);

        tracking.record(12L, EventType.PRODUCT_VIEW, EventSource.DETAIL,
                new TrackingContext(null, "a".repeat(64), false, false));

        verify(events, never()).save(any(ProductEvent.class));
    }

    @Test
    void recordsEverySuccessfulAddToCartWithoutDedupeLookup() {
        when(products.existsByIdAndActiveTrue(12L)).thenReturn(true);
        TrackingContext customer = new TrackingContext(7L, "a".repeat(64), false, false);

        tracking.record(12L, EventType.ADD_TO_CART, EventSource.LISTING, customer);
        tracking.record(12L, EventType.ADD_TO_CART, EventSource.LISTING, customer);

        verify(events, never()).existsByProductIdAndEventTypeAndUserIdAndCreatedAtGreaterThanEqual(
                any(), any(), any(), any());
        verify(events, org.mockito.Mockito.times(2)).save(any(ProductEvent.class));
    }

    @Test
    void databaseFailuresDoNotEscapeTrackingCall() {
        when(products.existsByIdAndActiveTrue(12L)).thenThrow(new IllegalStateException("database unavailable"));

        assertDoesNotThrow(() -> tracking.record(12L, EventType.ADD_TO_CART, EventSource.LISTING,
                new TrackingContext(null, "a".repeat(64), false, false)));
    }
}
