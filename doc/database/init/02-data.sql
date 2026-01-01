-- ==========================================
-- E-Commerce Platform Initial Data
-- ==========================================
-- 테스트 및 개발용 초기 데이터
-- 실행 순서: 01-schema.sql -> 02-data.sql

USE commerce;

-- ==========================================
-- 1. User Point (테스트 사용자 10명)
-- ==========================================
INSERT INTO user_point (id, balance, version, created_at, updated_at) VALUES
(1, 100000, 0, NOW(), NOW()),   -- 사용자 1: 10만원
(2, 500000, 0, NOW(), NOW()),   -- 사용자 2: 50만원
(3, 50000, 0, NOW(), NOW()),    -- 사용자 3: 5만원
(4, 1000000, 0, NOW(), NOW()),  -- 사용자 4: 100만원
(5, 0, 0, NOW(), NOW()),        -- 사용자 5: 포인트 없음
(6, 250000, 0, NOW(), NOW()),   -- 사용자 6: 25만원
(7, 750000, 0, NOW(), NOW()),   -- 사용자 7: 75만원
(8, 30000, 0, NOW(), NOW()),    -- 사용자 8: 3만원
(9, 150000, 0, NOW(), NOW()),   -- 사용자 9: 15만원
(10, 2000000, 0, NOW(), NOW()); -- 사용자 10: 200만원

-- ==========================================
-- 2. Product (상품 10개)
-- ==========================================
INSERT INTO product (id, name, description, base_price) VALUES
(1, '스마트폰 Galaxy S24', '최신 플래그십 스마트폰, 6.8인치 AMOLED 디스플레이', 1200000),
(2, '노트북 MacBook Pro 16', 'M3 Pro 칩, 16GB RAM, 512GB SSD', 3200000),
(3, '무선 이어폰 AirPods Pro', '액티브 노이즈 캔슬링, 공간 오디오 지원', 359000),
(4, '태블릿 iPad Air', '10.9인치 Liquid Retina 디스플레이, M1 칩', 899000),
(5, '스마트워치 Apple Watch Series 9', 'GPS + Cellular, 45mm', 649000),
(6, '게이밍 마우스 Logitech G Pro', '무선, HERO 25K 센서, 경량 디자인', 149000),
(7, '기계식 키보드 Keychron K8', 'RGB 백라이트, 핫스왑 가능', 129000),
(8, '모니터 LG UltraGear 27', '27인치 4K, 144Hz, G-Sync 지원', 589000),
(9, '외장 SSD Samsung T7', '1TB, USB 3.2 Gen2, 읽기 1050MB/s', 159000),
(10, '웹캠 Logitech C920', 'Full HD 1080p, 자동 초점, 스테레오 마이크', 89000);

-- ==========================================
-- 3. Product Option (상품별 2-3개 옵션, 재고 포함)
-- ==========================================
INSERT INTO product_option (id, product_id, option_name, price, quantity) VALUES
-- 스마트폰 Galaxy S24 옵션
(1, 1, '블랙 256GB', 1200000, 50),
(2, 1, '실버 256GB', 1200000, 30),
(3, 1, '블랙 512GB', 1400000, 20),

-- 노트북 MacBook Pro 16 옵션
(4, 2, '스페이스 그레이 512GB', 3200000, 15),
(5, 2, '실버 512GB', 3200000, 10),
(6, 2, '스페이스 그레이 1TB', 3600000, 5),

-- 무선 이어폰 AirPods Pro 옵션
(7, 3, '화이트', 359000, 100),

-- 태블릿 iPad Air 옵션
(8, 4, '스페이스 그레이 64GB', 899000, 40),
(9, 4, '스타라이트 64GB', 899000, 35),
(10, 4, '스페이스 그레이 256GB', 1099000, 25),

-- 스마트워치 Apple Watch Series 9 옵션
(11, 5, '미드나이트 알루미늄', 649000, 60),
(12, 5, '스타라이트 알루미늄', 649000, 45),
(13, 5, '실버 스테인리스 스틸', 949000, 15),

-- 게이밍 마우스 Logitech G Pro 옵션
(14, 6, '블랙', 149000, 80),
(15, 6, '화이트', 149000, 70),

-- 기계식 키보드 Keychron K8 옵션
(16, 7, '갈축 RGB', 129000, 50),
(17, 7, '청축 RGB', 129000, 40),
(18, 7, '적축 RGB', 129000, 45),

-- 모니터 LG UltraGear 27 옵션
(19, 8, '27인치 4K', 589000, 30),

-- 외장 SSD Samsung T7 옵션
(20, 9, '블랙 1TB', 159000, 100),
(21, 9, '블루 1TB', 159000, 90),
(22, 9, '레드 1TB', 159000, 85),

-- 웹캠 Logitech C920 옵션
(23, 10, '블랙', 89000, 120);

-- ==========================================
-- 4. Coupon (다양한 타입의 쿠폰 5개)
-- ==========================================
INSERT INTO coupon (id, coupon_name, discount_type, discount_value, total_quantity, issued_quantity, limit_per_user, duration, min_order_value, valid_from, valid_until, created_at) VALUES
-- 신규 가입 쿠폰 (10% 할인)
(1, '신규 가입 환영 쿠폰', 'PERCENTAGE', 10, 1000, 0, 1, 30, 50000, '2024-01-01', '2026-12-31', NOW()),

-- 고정 할인 쿠폰 (5만원 할인)
(2, '봄맞이 특가 쿠폰', 'FIXED', 50000, 500, 0, 2, 30, 300000, '2024-03-01', '2026-05-31', NOW()),

-- 대량 할인 쿠폰 (20% 할인)
(3, 'VIP 회원 전용 쿠폰', 'PERCENTAGE', 20, 100, 0, 1, 60, 500000, '2024-01-01', '2026-12-31', NOW()),

-- 소액 할인 쿠폰 (1만원 할인)
(4, '첫 구매 감사 쿠폰', 'FIXED', 10000, 2000, 0, 1, 14, 100000, '2024-01-01', '2026-12-31', NOW()),

-- 고액 할인 쿠폰 (15% 할인)
(5, '블랙프라이데이 쿠폰', 'PERCENTAGE', 15, 300, 0, 3, 7, 1000000, '2024-11-01', '2026-11-30', NOW());

-- ==========================================
-- 5. User Coupon (일부 사용자에게 쿠폰 발급)
-- ==========================================
-- 사용자 1: 신규 가입 쿠폰 발급
INSERT INTO user_coupon (id, user_id, coupon_id, status, used_at, expired_at, version, created_at) VALUES
(1, 1, 1, 'ISSUED', NULL, DATE_ADD(CURDATE(), INTERVAL 30 DAY), 0, NOW());

-- 사용자 2: 봄맞이 특가 쿠폰 발급
INSERT INTO user_coupon (id, user_id, coupon_id, status, used_at, expired_at, version, created_at) VALUES
(2, 2, 2, 'ISSUED', NULL, DATE_ADD(CURDATE(), INTERVAL 30 DAY), 0, NOW());

-- 사용자 3: 첫 구매 감사 쿠폰 발급 (이미 사용함)
INSERT INTO user_coupon (id, user_id, coupon_id, status, used_at, expired_at, version, created_at) VALUES
(3, 3, 4, 'USED', CURDATE(), DATE_ADD(CURDATE(), INTERVAL 14 DAY), 0, NOW());

-- 사용자 4: VIP 회원 전용 쿠폰 발급
INSERT INTO user_coupon (id, user_id, coupon_id, status, used_at, expired_at, version, created_at) VALUES
(4, 4, 3, 'ISSUED', NULL, DATE_ADD(CURDATE(), INTERVAL 60 DAY), 0, NOW());

-- 사용자 6: 신규 가입 쿠폰 발급
INSERT INTO user_coupon (id, user_id, coupon_id, status, used_at, expired_at, version, created_at) VALUES
(5, 6, 1, 'ISSUED', NULL, DATE_ADD(CURDATE(), INTERVAL 30 DAY), 0, NOW());

-- 쿠폰 발급 수 업데이트
UPDATE coupon SET issued_quantity = 2 WHERE id = 1;  -- 신규 가입 쿠폰 2개 발급
UPDATE coupon SET issued_quantity = 1 WHERE id = 2;  -- 봄맞이 특가 쿠폰 1개 발급
UPDATE coupon SET issued_quantity = 1 WHERE id = 3;  -- VIP 회원 전용 쿠폰 1개 발급
UPDATE coupon SET issued_quantity = 1 WHERE id = 4;  -- 첫 구매 감사 쿠폰 1개 발급

-- ==========================================
-- 데이터 삽입 완료
-- ==========================================
-- 총 10명의 사용자, 10개의 상품, 23개의 상품 옵션, 5개의 쿠폰, 5개의 사용자 쿠폰 생성
