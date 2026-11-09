package iuh.fit.demoprj.tracking.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import iuh.fit.demoprj.tracking.model.EventSource;
import iuh.fit.demoprj.tracking.model.EventType;
import iuh.fit.demoprj.tracking.model.ProductEvent;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
        LocalDateTime cutoff = LocalDateTime.of(2026, 11, 9, 10, 0);
        events.saveAndFlush(new ProductEvent(12L, EventType.PRODUCT_VIEW, null, "a".repeat(64),
                EventSource.DETAIL, cutoff.minusSeconds(1)));
        events.saveAndFlush(new ProductEvent(12L, EventType.ADD_TO_CART, null, "a".repeat(64),
                EventSource.LISTING, cutoff));

        assertEquals(1, events.deleteByCreatedAtBefore(cutoff));
        assertEquals(1, events.count());
        assertEquals(EventType.ADD_TO_CART, events.findAll().get(0).getEventType());
    }
}
