-- DDL for integration tests (no foreign key constraints)
-- Generated from entity classes

-- Product table
CREATE TABLE IF NOT EXISTS product (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    base_price BIGINT NOT NULL
);

-- Product Option table
CREATE TABLE IF NOT EXISTS product_option (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    option_name VARCHAR(255) NOT NULL,
    price BIGINT NOT NULL,
    quantity BIGINT NOT NULL
);

-- User Point table
CREATE TABLE IF NOT EXISTS user_point (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    balance BIGINT NOT NULL,
    version BIGINT,
    created_at DATETIME(6),
    updated_at DATETIME(6)
);

-- Point History table
CREATE TABLE IF NOT EXISTS point_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    amount BIGINT NOT NULL,
    balance_after BIGINT NOT NULL,
    description VARCHAR(255),
    created_at DATETIME(6)
);

-- Coupon table
CREATE TABLE IF NOT EXISTS coupon (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    coupon_name VARCHAR(255) NOT NULL,
    discount_type VARCHAR(50) NOT NULL,
    discount_value INT NOT NULL,
    total_quantity INT NOT NULL,
    issued_quantity INT NOT NULL,
    limit_per_user INT NOT NULL,
    duration INT NOT NULL,
    min_order_value INT NOT NULL,
    valid_from DATE NOT NULL,
    valid_until DATE NOT NULL,
    created_at DATETIME(6)
);

-- User Coupon table
CREATE TABLE IF NOT EXISTS user_coupon (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    coupon_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    used_at DATE,
    expired_at DATE NOT NULL,
    version BIGINT,
    created_at DATETIME(6)
);

-- Orders table
CREATE TABLE IF NOT EXISTS orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    user_coupon_id BIGINT,
    status VARCHAR(50) NOT NULL,
    total_amount BIGINT NOT NULL,
    discount_amount BIGINT NOT NULL,
    use_point_amount BIGINT NOT NULL,
    final_amount BIGINT NOT NULL,
    paid_at DATETIME(6),
    version BIGINT,
    created_at DATETIME(6)
);

-- Order Item table
CREATE TABLE IF NOT EXISTS order_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_option_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    unit_price BIGINT NOT NULL,
    subtotal BIGINT NOT NULL,
    created_at DATETIME(6)
);

-- Outbox Events table
CREATE TABLE IF NOT EXISTS outbox_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    aggregate_type VARCHAR(50) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    retry_count INT NOT NULL,
    max_retry INT NOT NULL,
    published_at DATETIME(6),
    last_retry_at DATETIME(6),
    updated_at DATETIME(6),
    error_message TEXT,
    created_at DATETIME(6),
    INDEX idx_status_created (status, created_at),
    INDEX idx_aggregate (aggregate_type, aggregate_id),
    INDEX idx_status_updated (status, updated_at)
);

-- Consumed Event Log table
CREATE TABLE IF NOT EXISTS consumed_event_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    processed_at DATETIME(6) NOT NULL,
    consumer_name VARCHAR(100) NOT NULL,
    payload TEXT,
    created_at DATETIME(6),
    INDEX idx_consumed_created (created_at),
    INDEX idx_consumed_event_type (event_type, created_at),
    UNIQUE KEY uk_consumed_event (event_id, event_type)
);
