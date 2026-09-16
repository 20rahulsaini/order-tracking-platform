-- Schema for the Tracking Service's database (tracking_db)

-- Append-only history of every status-change event we have ever applied.
CREATE TABLE IF NOT EXISTS order_status_history (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id      VARCHAR(32)  NOT NULL,
    status        VARCHAR(32)  NOT NULL,
    event_type    VARCHAR(64)  NOT NULL,
    event_id      VARCHAR(64)  NOT NULL,
    occurred_at   DATETIME(6)  NOT NULL,
    recorded_at   DATETIME(6)  NOT NULL,
    UNIQUE KEY uk_history_event (event_id),
    INDEX idx_history_order_status (order_id, status),
    INDEX idx_history_order_occurred (order_id, occurred_at)
);

-- Idempotency ledger. One row per event that has been successfully
-- applied. The unique constraint on event_id is the safety net against
-- concurrent consumer races (check-then-insert is not atomic in Java).
CREATE TABLE IF NOT EXISTS processed_events (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id      VARCHAR(64) NOT NULL,
    order_id      VARCHAR(32) NOT NULL,
    status        VARCHAR(32),
    processed_at  DATETIME(6) NOT NULL,
    UNIQUE KEY uk_processed_event_id (event_id),
    INDEX idx_processed_order (order_id)
);
