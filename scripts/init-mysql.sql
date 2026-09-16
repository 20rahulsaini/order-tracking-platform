-- ===========================================================
--  MySQL init script - runs on first boot only.
--  Creates the tracking_db database + user, and applies the
--  per-database schemas. (The order_db database + user are
--  created automatically by the MYSQL_DATABASE / MYSQL_USER env
--  vars on the container; here we just need to create the second
--  database + the schema objects.)
-- ===========================================================

-- Tracking Service database + user (order_db/orderuser come from env)
CREATE DATABASE IF NOT EXISTS tracking_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'trackinguser'@'%' IDENTIFIED BY 'trackingpass';
GRANT ALL PRIVILEGES ON tracking_db.* TO 'trackinguser'@'%';

-- Reload grants so the tracking service can connect immediately on first boot
FLUSH PRIVILEGES;

-- ---- order_db schema (applied inside the auto-created database) ----
USE order_db;

CREATE TABLE IF NOT EXISTS orders (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id      VARCHAR(32)  NOT NULL UNIQUE,
    customer_name VARCHAR(128) NOT NULL,
    product       VARCHAR(128) NOT NULL,
    quantity      INT          NOT NULL,
    total_amount  DECIMAL(12, 2) NOT NULL,
    status        VARCHAR(32)  NOT NULL,
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,
    INDEX idx_orders_status (status),
    INDEX idx_orders_created_at (created_at)
);

-- ---- tracking_db schema ----
USE tracking_db;

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

CREATE TABLE IF NOT EXISTS processed_events (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id      VARCHAR(64) NOT NULL,
    order_id      VARCHAR(32) NOT NULL,
    status        VARCHAR(32),
    processed_at  DATETIME(6) NOT NULL,
    UNIQUE KEY uk_processed_event_id (event_id),
    INDEX idx_processed_order (order_id)
);
