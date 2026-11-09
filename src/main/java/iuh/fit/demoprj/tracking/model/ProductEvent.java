package iuh.fit.demoprj.tracking.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "product_events", indexes = {
        @Index(name = "idx_pe_product_type_time", columnList = "product_id,event_type,created_at"),
        @Index(name = "idx_pe_time", columnList = "created_at"),
        @Index(name = "idx_pe_dedupe_session", columnList = "session_hash,product_id,event_type,created_at"),
        @Index(name = "idx_pe_dedupe_user", columnList = "user_id,product_id,event_type,created_at")
})
public class ProductEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 20)
    private EventType eventType;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "session_hash", nullable = false, length = 64)
    private String sessionHash;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private EventSource source;

    @Column(name = "created_at", nullable = false, columnDefinition = "datetime")
    private LocalDateTime createdAt;

    protected ProductEvent() {
    }

    public ProductEvent(Long productId, EventType eventType, Long userId, String sessionHash,
                        EventSource source, LocalDateTime createdAt) {
        this.productId = productId;
        this.eventType = eventType;
        this.userId = userId;
        this.sessionHash = sessionHash;
        this.source = source;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Long getProductId() {
        return productId;
    }

    public EventType getEventType() {
        return eventType;
    }

    public Long getUserId() {
        return userId;
    }

    public String getSessionHash() {
        return sessionHash;
    }

    public EventSource getSource() {
        return source;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
