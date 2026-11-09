CREATE TABLE IF NOT EXISTS product_events (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    event_type VARCHAR(20) NOT NULL,
    user_id BIGINT NULL,
    session_hash CHAR(64) NOT NULL,
    source VARCHAR(20) NULL,
    created_at DATETIME NOT NULL,
    INDEX idx_pe_product_type_time (product_id, event_type, created_at),
    INDEX idx_pe_time (created_at),
    INDEX idx_pe_dedupe_session (session_hash, product_id, event_type, created_at),
    INDEX idx_pe_dedupe_user (user_id, product_id, event_type, created_at)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
