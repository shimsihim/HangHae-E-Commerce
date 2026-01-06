-- ==========================================
-- E-Commerce Platform Database Schema
-- ==========================================
-- 실행 순서: 01-schema.sql -> 02-data.sql
-- Docker MySQL 컨테이너 최초 실행 시 자동으로 실행됨
--
-- 설계 원칙:
-- 1. 외래키 제약조건 없음 (애플리케이션 레벨에서 관리)
-- 2. 최소한의 인덱스만 사용 (조회 성능에 필수적인 것만)
-- 3. user_point가 사실상 유저 테이블 역할

USE commerce;

-- ==========================================
-- 1. User Point (사용자 포인트 / 사실상 유저 테이블)
-- ==========================================
CREATE TABLE IF NOT EXISTS user_point (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '사용자 ID (PK)',
    balance BIGINT NOT NULL DEFAULT 0 COMMENT '현재 포인트 잔액',
    version BIGINT DEFAULT 0 COMMENT '낙관적 락 버전 (동시성 제어)',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성 일시',
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정 일시'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='사용자 포인트 (사실상 유저 테이블)';

-- ==========================================
-- 2. Point History (포인트 사용/충전 내역)
-- ==========================================
CREATE TABLE IF NOT EXISTS point_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '포인트 내역 ID (PK)',
    user_id BIGINT NOT NULL COMMENT '사용자 ID (user_point.id)',
    type VARCHAR(20) NOT NULL COMMENT '거래 타입 (CHARGE: 충전, USE: 사용)',
    amount BIGINT NOT NULL COMMENT '거래 금액',
    balance_after BIGINT NOT NULL COMMENT '거래 후 잔액',
    description VARCHAR(255) COMMENT '거래 설명',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '거래 일시',

    INDEX idx_user_id (user_id) COMMENT '사용자별 포인트 내역 조회용'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='포인트 충전/사용 내역';

-- ==========================================
-- 3. Product (상품)
-- ==========================================
CREATE TABLE IF NOT EXISTS product (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '상품 ID (PK)',
    name VARCHAR(255) NOT NULL COMMENT '상품명',
    description TEXT COMMENT '상품 설명',
    base_price BIGINT NOT NULL COMMENT '기본 가격'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='상품 마스터';

-- ==========================================
-- 4. Product Option (상품 옵션/재고)
-- ==========================================
CREATE TABLE IF NOT EXISTS product_option (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '상품 옵션 ID (PK)',
    product_id BIGINT NOT NULL COMMENT '상품 ID (product.id)',
    option_name VARCHAR(255) NOT NULL COMMENT '옵션명',
    price BIGINT NOT NULL COMMENT '옵션 가격',
    quantity BIGINT NOT NULL DEFAULT 0 COMMENT '재고 수량',

    INDEX idx_product_id (product_id) COMMENT '상품별 옵션 조회용'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='상품 옵션 및 재고';

-- ==========================================
-- 5. Coupon (쿠폰 마스터)
-- ==========================================
CREATE TABLE IF NOT EXISTS coupon (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '쿠폰 ID (PK)',
    coupon_name VARCHAR(255) NOT NULL COMMENT '쿠폰명',
    discount_type VARCHAR(20) NOT NULL COMMENT '할인 타입 (PERCENTAGE, FIXED)',
    discount_value INT NOT NULL COMMENT '할인 값',
    total_quantity INT NOT NULL COMMENT '총 발급 가능 수량',
    issued_quantity INT NOT NULL DEFAULT 0 COMMENT '현재까지 발급된 수량',
    limit_per_user INT NOT NULL COMMENT '사용자당 발급 제한',
    duration INT NOT NULL COMMENT '유효 기간 (일)',
    min_order_value INT NOT NULL COMMENT '최소 주문 금액',
    valid_from DATE NOT NULL COMMENT '발급 시작일',
    valid_until DATE NOT NULL COMMENT '발급 종료일',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성 일시'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='쿠폰 마스터';

-- ==========================================
-- 6. User Coupon (사용자별 발급된 쿠폰)
-- ==========================================
CREATE TABLE IF NOT EXISTS user_coupon (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '사용자 쿠폰 ID (PK)',
    user_id BIGINT NOT NULL COMMENT '사용자 ID (user_point.id)',
    coupon_id BIGINT NOT NULL COMMENT '쿠폰 ID (coupon.id)',
    status VARCHAR(20) NOT NULL COMMENT '상태 (ISSUED, USED, EXPIRED)',
    used_at DATE COMMENT '사용 일자',
    expired_at DATE NOT NULL COMMENT '만료 일자',
    version BIGINT DEFAULT 0 COMMENT '낙관적 락 버전',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '발급 일시',

    INDEX idx_user_id (user_id) COMMENT '사용자별 쿠폰 조회용',
    INDEX idx_user_coupon (user_id, coupon_id) COMMENT '사용자별 쿠폰 중복 체크용'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='사용자별 발급된 쿠폰';

-- ==========================================
-- 7. Orders (주문)
-- ==========================================
CREATE TABLE IF NOT EXISTS orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '주문 ID (PK)',
    user_id BIGINT NOT NULL COMMENT '주문한 사용자 ID (user_point.id)',
    user_coupon_id BIGINT COMMENT '사용한 쿠폰 ID (user_coupon.id)',
    status VARCHAR(20) NOT NULL COMMENT '주문 상태 (PENDING, PAID, CANCELLED)',
    total_amount BIGINT NOT NULL COMMENT '총 주문 금액',
    discount_amount BIGINT NOT NULL DEFAULT 0 COMMENT '쿠폰 할인 금액',
    use_point_amount BIGINT NOT NULL DEFAULT 0 COMMENT '사용한 포인트',
    final_amount BIGINT NOT NULL COMMENT '최종 결제 금액',
    paid_at DATETIME(6) COMMENT '결제 완료 일시',
    version BIGINT DEFAULT 0 COMMENT '낙관적 락 버전',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '주문 생성 일시',

    INDEX idx_user_id (user_id) COMMENT '사용자별 주문 조회용',
    INDEX idx_created_at (created_at) COMMENT '주문 일시별 조회용'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='주문';

-- ==========================================
-- 8. Order Item (주문 상품 목록)
-- ==========================================
CREATE TABLE IF NOT EXISTS order_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '주문 상품 ID (PK)',
    order_id BIGINT NOT NULL COMMENT '주문 ID (orders.id)',
    product_id BIGINT NOT NULL COMMENT '상품 ID (product.id)',
    product_option_id BIGINT NOT NULL COMMENT '상품 옵션 ID (product_option.id)',
    quantity INT NOT NULL COMMENT '주문 수량',
    unit_price BIGINT NOT NULL COMMENT '단가',
    subtotal BIGINT NOT NULL COMMENT '소계',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성 일시',

    INDEX idx_order_id (order_id) COMMENT '주문별 상품 조회용'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='주문 상품 명세';

-- ==========================================
-- 9. Outbox Events (트랜잭션 아웃박스 패턴)
-- ==========================================
CREATE TABLE IF NOT EXISTS outbox_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '아웃박스 이벤트 ID (PK)',
    aggregate_type VARCHAR(50) NOT NULL COMMENT '집합체 타입 (ORDER, COUPON, POINT)',
    aggregate_id VARCHAR(100) NOT NULL COMMENT '집합체 ID',
    event_type VARCHAR(100) NOT NULL COMMENT '이벤트 타입',
    payload TEXT NOT NULL COMMENT '이벤트 데이터 (JSON)',
    status VARCHAR(20) NOT NULL COMMENT '발행 상태 (PENDING, PUBLISHED, DEAD_LETTER)',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '재시도 횟수',
    max_retry INT NOT NULL DEFAULT 3 COMMENT '최대 재시도 횟수',
    published_at DATETIME(6) COMMENT 'Kafka 발행 완료 일시',
    last_retry_at DATETIME(6) COMMENT '마지막 재시도 일시',
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정 일시',
    error_message TEXT COMMENT '에러 메시지',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성 일시',

    -- Outbox Poller가 미발행 이벤트를 조회하는데 필수적인 인덱스
    INDEX idx_status_created (status, created_at) COMMENT '미발행 이벤트 조회 (Poller 성능)'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='트랜잭션 아웃박스 패턴';

-- ==========================================
-- 10. Consumed Event Log (이벤트 소비 로그 - 멱등성 보장)
-- ==========================================
CREATE TABLE IF NOT EXISTS consumed_event_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '소비 로그 ID (PK)',
    event_id VARCHAR(100) NOT NULL COMMENT '이벤트 식별자 (aggregateId 등)',
    event_type VARCHAR(50) NOT NULL COMMENT '이벤트 타입 (CouponIssued, OrderCompleted 등)',
    processed_at DATETIME(6) NOT NULL COMMENT '이벤트 처리 시간',
    consumer_name VARCHAR(100) NOT NULL COMMENT 'Consumer 이름 (디버깅용)',
    payload TEXT COMMENT '원본 JSON 페이로드 (디버깅용)',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성 일시',

    -- 멱등성 보장을 위한 유니크 제약 (같은 이벤트 중복 처리 방지)
    UNIQUE KEY uk_consumed_event (event_id, event_type),

    INDEX idx_consumed_created (created_at) COMMENT '로그 조회용',
    INDEX idx_consumed_event_type (event_type, created_at) COMMENT '이벤트 타입별 조회용'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='이벤트 소비 로그 (멱등성 보장)';

-- ==========================================
-- 스키마 생성 완료
-- ==========================================
