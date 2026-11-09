package iuh.fit.demoprj.tracking.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import iuh.fit.demoprj.tracking.model.EventSource;
import iuh.fit.demoprj.tracking.model.EventType;
import iuh.fit.demoprj.tracking.model.ProductEvent;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class ProductEventRepositoryTest {
    @Autowired
    private ProductEventRepository events;

    @Test
    void persistsEventsAndFindsDuplicatesByAccountOrGuestSession() {
        LocalDateTime now = LocalDateTime.of(2026, 11, 9, 10, 0);
        events.saveAndFlush(new ProductEvent(12L, EventType.PRODUCT_VIEW, 7L, "a".repeat(64),
                EventSource.DETAIL, now));
        events.saveAndFlush(new ProductEvent(12L, EventType.PRODUCT_CLICK, null, "b".repeat(64),
                EventSource.LISTING, now));

        assertTrue(events.existsByProductIdAndEventTypeAndUserIdAndCreatedAtGreaterThanEqual(
                12L, EventType.PRODUCT_VIEW, 7L, now.minusMinutes(30)));
        assertTrue(events.existsByProductIdAndEventTypeAndSessionHashAndCreatedAtGreaterThanEqual(
                12L, EventType.PRODUCT_CLICK, "b".repeat(64), now.minusMinutes(30)));
        assertFalse(events.existsByProductIdAndEventTypeAndSessionHashAndCreatedAtGreaterThanEqual(
                12L, EventType.PRODUCT_CLICK, "c".repeat(64), now.minusMinutes(30)));
        assertEquals(EventSource.DETAIL, events.findAll().get(0).getSource());
    }

    @Test
    void deletesEventsOlderThanTheRetentionCutoff() {
        LocalDateTime cutoff = LocalDateTime.of(1900, 1, 1, 0, 0);
        ProductEvent oldEvent = events.saveAndFlush(new ProductEvent(12L, EventType.PRODUCT_VIEW, null,
                "a".repeat(64), EventSource.DETAIL, cutoff.minusSeconds(1)));
        ProductEvent boundaryEvent = events.saveAndFlush(new ProductEvent(12L, EventType.ADD_TO_CART, null,
                "a".repeat(64), EventSource.LISTING, cutoff));

        assertEquals(1, events.deleteByCreatedAtBefore(cutoff));
        assertFalse(events.existsById(oldEvent.getId()));
        assertTrue(events.existsById(boundaryEvent.getId()));
    }

    @Test
    void aggregatesEventCountsByProductAndTypeOverHalfOpenRange() {
        LocalDateTime from = LocalDateTime.of(2099, 1, 1, 0, 0);
        LocalDateTime to = from.plusDays(1);
        events.saveAndFlush(new ProductEvent(900_001L, EventType.PRODUCT_VIEW, null, "a".repeat(64),
                EventSource.DETAIL, from));
        events.saveAndFlush(new ProductEvent(900_001L, EventType.PRODUCT_VIEW, null, "b".repeat(64),
                EventSource.DETAIL, to.minusSeconds(1)));
        events.saveAndFlush(new ProductEvent(900_001L, EventType.PRODUCT_CLICK, null, "c".repeat(64),
                EventSource.HOME, to.minusSeconds(1)));
        events.saveAndFlush(new ProductEvent(900_002L, EventType.PRODUCT_VIEW, null, "d".repeat(64),
                EventSource.DETAIL, to));

        var counts = events.countByProductAndEventTypeBetween(from, to);

        assertEquals(2, counts.size());
        assertTrue(counts.stream().anyMatch(row -> row.getProductId().equals(900_001L)
                && row.getEventType() == EventType.PRODUCT_VIEW && row.getEventCount() == 2));
        assertTrue(counts.stream().anyMatch(row -> row.getProductId().equals(900_001L)
                && row.getEventType() == EventType.PRODUCT_CLICK && row.getEventCount() == 1));
    }

    @Test
    void returnsDistinctRecentlyViewedProductsInDescendingViewTime() {
        LocalDateTime viewedAt = LocalDateTime.of(2099, 3, 1, 0, 0);
        events.saveAndFlush(new ProductEvent(901_001L, EventType.PRODUCT_VIEW, 901L, "a".repeat(64),
                EventSource.DETAIL, viewedAt));
        events.saveAndFlush(new ProductEvent(901_002L, EventType.PRODUCT_VIEW, 901L, "b".repeat(64),
                EventSource.DETAIL, viewedAt.plusMinutes(1)));
        events.saveAndFlush(new ProductEvent(901_001L, EventType.PRODUCT_VIEW, 901L, "c".repeat(64),
                EventSource.DETAIL, viewedAt.plusMinutes(2)));
        events.saveAndFlush(new ProductEvent(901_003L, EventType.PRODUCT_CLICK, 901L, "d".repeat(64),
                EventSource.LISTING, viewedAt.plusMinutes(3)));

        var recent = events.findRecentProductViewsByUserId(
                901L, EventType.PRODUCT_VIEW, PageRequest.of(0, 10));

        assertEquals(List.of(901_001L, 901_002L),
                recent.stream().map(ProductEventRepository.RecentProductViewProjection::getProductId).toList());
    }
}
