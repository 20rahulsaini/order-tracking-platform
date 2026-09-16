-- Schema for the Order Service's database (order_db)
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
