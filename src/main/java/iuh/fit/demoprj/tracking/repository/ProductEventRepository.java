package iuh.fit.demoprj.tracking.repository;

import iuh.fit.demoprj.tracking.model.EventType;
import iuh.fit.demoprj.tracking.model.ProductEvent;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductEventRepository extends JpaRepository<ProductEvent, Long> {
    boolean existsByProductIdAndEventTypeAndUserIdAndCreatedAtGreaterThanEqual(
            Long productId, EventType eventType, Long userId, LocalDateTime createdAt);

    boolean existsByProductIdAndEventTypeAndSessionHashAndCreatedAtGreaterThanEqual(
            Long productId, EventType eventType, String sessionHash, LocalDateTime createdAt);

    long deleteByCreatedAtBefore(LocalDateTime createdAt);

    @Query("""
            select event.productId as productId, event.eventType as eventType, count(event.id) as eventCount
            from ProductEvent event
            where event.createdAt >= :from and event.createdAt < :to
            group by event.productId, event.eventType
            """)
    List<EventCountProjection> countByProductAndEventTypeBetween(
            @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("""
            select event.productId as productId, max(event.createdAt) as lastViewedAt
            from ProductEvent event
            where event.userId = :userId and event.eventType = :eventType
            group by event.productId
            order by max(event.createdAt) desc, event.productId desc
            """)
    List<RecentProductViewProjection> findRecentProductViewsByUserId(
            @Param("userId") Long userId, @Param("eventType") EventType eventType, Pageable pageable);

    interface EventCountProjection {
        Long getProductId();
        EventType getEventType();
        Long getEventCount();
    }

    interface RecentProductViewProjection {
        Long getProductId();
        LocalDateTime getLastViewedAt();
    }
}
