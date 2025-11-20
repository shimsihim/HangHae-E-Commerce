package io.hhplus.tdd.domain.order.application;

import io.hhplus.tdd.domain.BaseIntegrationTest;
import io.hhplus.tdd.domain.coupon.domain.model.Coupon;
import io.hhplus.tdd.domain.coupon.domain.model.DiscountType;
import io.hhplus.tdd.domain.coupon.domain.model.Status;
import io.hhplus.tdd.domain.coupon.domain.model.UserCoupon;
import io.hhplus.tdd.domain.order.domain.model.Order;
import io.hhplus.tdd.domain.order.domain.model.OrderStatus;
import io.hhplus.tdd.domain.point.domain.model.UserPoint;
import io.hhplus.tdd.domain.point.infrastructure.repository.UserPointRepository;
import io.hhplus.tdd.domain.product.domain.model.Product;
import io.hhplus.tdd.domain.product.domain.model.ProductOption;
import io.hhplus.tdd.domain.product.infrastructure.repository.ProductOptionRepository;
import io.hhplus.tdd.domain.product.infrastructure.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
class CreateOrderIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private CreateOrderUseCase createOrderUseCase;

    @Autowired
    private UserPointRepository userPointRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductOptionRepository productOptionRepository;

    @Test
    @DisplayName("주문 생성 통합 테스트 - 정상적으로 주문이 생성된다")
    void 주문_생성_성공() {
        // given
        UserPoint userPoint = UserPoint.builder()
                .balance(100000L)
                .build();
        UserPoint savedUserPoint = userPointRepository.save(userPoint);

        Product product = Product.builder()
                .name("테스트 상품")
                .description("테스트용 상품")
                .basePrice(10000L)
                .build();
        Product savedProduct = productRepository.save(product);

        ProductOption productOption = ProductOption.builder()
                .product(savedProduct)
                .optionName("기본 옵션")
                .price(10000L)
                .quantity(100L)
                .build();
        ProductOption savedOption = productOptionRepository.save(productOption);

        CreateOrderUseCase.Input input = new CreateOrderUseCase.Input(
                savedUserPoint.getId(),
                Arrays.asList(
                        new CreateOrderUseCase.Input.ProductInfo(savedOption.getId(), 2)
                ),
                null,
                0L
        );

        // when
        CreateOrderUseCase.Output result = createOrderUseCase.execute(input);

        // then
        assertThat(result.orderId()).isNotNull();
        assertThat(result.userId()).isEqualTo(savedUserPoint.getId());
        assertThat(result.totalAmount()).isEqualTo(20000L);
        assertThat(result.discountAmount()).isEqualTo(0L);
        assertThat(result.usePointAmount()).isEqualTo(0L);
        assertThat(result.finalAmount()).isEqualTo(20000L);

        Order order = orderRepository.findById(result.orderId()).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    @DisplayName("주문 생성 통합 테스트 - 쿠폰 적용 주문")
    void 쿠폰_적용_주문_생성() {
        // given
        UserPoint userPoint = UserPoint.builder()
                .balance(100000L)
                .build();
        UserPoint savedUserPoint = userPointRepository.save(userPoint);

        Product product = Product.builder()
                .name("테스트 상품")
                .description("테스트용 상품")
                .basePrice(10000L)
                .build();
        Product savedProduct = productRepository.save(product);

        ProductOption productOption = ProductOption.builder()
                .product(savedProduct)
                .optionName("기본 옵션")
                .price(10000L)
                .quantity(100L)
                .build();
        ProductOption savedOption = productOptionRepository.save(productOption);

        Coupon coupon = Coupon.builder()
                .couponName("테스트 쿠폰")
                .discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(5000)
                .totalQuantity(100)
                .issuedQuantity(0)
                .limitPerUser(1)
                .duration(30)
                .minOrderValue(10000)
                .validFrom(LocalDate.now().minusDays(1))
                .validUntil(LocalDate.now().plusDays(30))
                .build();
        Coupon savedCoupon = couponRepository.save(coupon);

        UserCoupon userCoupon = UserCoupon.builder()
                .userId(savedUserPoint.getId())
                .couponId(savedCoupon.getId())
                .coupon(savedCoupon)
                .status(Status.ISSUED)
                .expiredAt(LocalDate.now().plusDays(30))
                .build();
        UserCoupon savedUserCoupon = userCouponRepository.save(userCoupon);

        CreateOrderUseCase.Input input = new CreateOrderUseCase.Input(
                savedUserPoint.getId(),
                Arrays.asList(
                        new CreateOrderUseCase.Input.ProductInfo(savedOption.getId(), 2)
                ),
                savedUserCoupon.getId(),
                0L
        );

        // when
        CreateOrderUseCase.Output result = createOrderUseCase.execute(input);

        // then
        assertThat(result.orderId()).isNotNull();
        assertThat(result.totalAmount()).isEqualTo(20000L);
        assertThat(result.discountAmount()).isEqualTo(5000L);
        assertThat(result.finalAmount()).isEqualTo(15000L);
    }

    @Test
    @DisplayName("주문 생성 통합 테스트 - 포인트 사용 주문")
    void 포인트_사용_주문_생성() {
        // given
        UserPoint userPoint = UserPoint.builder()
                .balance(100000L)
                .build();
        UserPoint savedUserPoint = userPointRepository.save(userPoint);

        Product product = Product.builder()
                .name("테스트 상품")
                .description("테스트용 상품")
                .basePrice(10000L)
                .build();
        Product savedProduct = productRepository.save(product);

        ProductOption productOption = ProductOption.builder()
                .product(savedProduct)
                .optionName("기본 옵션")
                .price(10000L)
                .quantity(100L)
                .build();
        ProductOption savedOption = productOptionRepository.save(productOption);

        CreateOrderUseCase.Input input = new CreateOrderUseCase.Input(
                savedUserPoint.getId(),
                Arrays.asList(
                        new CreateOrderUseCase.Input.ProductInfo(savedOption.getId(), 2)
                ),
                null,
                5000L
        );

        // when
        CreateOrderUseCase.Output result = createOrderUseCase.execute(input);

        // then
        assertThat(result.orderId()).isNotNull();
        assertThat(result.totalAmount()).isEqualTo(20000L);
        assertThat(result.usePointAmount()).isEqualTo(5000L);
        assertThat(result.finalAmount()).isEqualTo(15000L);
    }

    @Test
    @DisplayName("주문 생성 통합 테스트 - 0원 결제 시 즉시 완료")
    void 영원_결제_시_즉시_완료() {
        // given
        UserPoint userPoint = UserPoint.builder()
                .balance(100000L)
                .build();
        UserPoint savedUserPoint = userPointRepository.save(userPoint);

        Product product = Product.builder()
                .name("테스트 상품")
                .description("테스트용 상품")
                .basePrice(10000L)
                .build();
        Product savedProduct = productRepository.save(product);

        ProductOption productOption = ProductOption.builder()
                .product(savedProduct)
                .optionName("기본 옵션")
                .price(10000L)
                .quantity(100L)
                .build();
        ProductOption savedOption = productOptionRepository.save(productOption);

        Coupon coupon = Coupon.builder()
                .couponName("100% 할인 쿠폰")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(100)
                .totalQuantity(100)
                .issuedQuantity(0)
                .limitPerUser(1)
                .duration(30)
                .minOrderValue(0)
                .validFrom(LocalDate.now().minusDays(1))
                .validUntil(LocalDate.now().plusDays(30))
                .build();
        Coupon savedCoupon = couponRepository.save(coupon);

        UserCoupon userCoupon = UserCoupon.builder()
                .userId(savedUserPoint.getId())
                .couponId(savedCoupon.getId())
                .coupon(savedCoupon)
                .status(Status.ISSUED)
                .expiredAt(LocalDate.now().plusDays(30))
                .build();
        UserCoupon savedUserCoupon = userCouponRepository.save(userCoupon);

        CreateOrderUseCase.Input input = new CreateOrderUseCase.Input(
                savedUserPoint.getId(),
                Arrays.asList(
                        new CreateOrderUseCase.Input.ProductInfo(savedOption.getId(), 1)
                ),
                savedUserCoupon.getId(),
                0L
        );

        // when
        CreateOrderUseCase.Output result = createOrderUseCase.execute(input);

        // then
        assertThat(result.finalAmount()).isEqualTo(0L);
        Order order = orderRepository.findById(result.orderId()).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);

        // 0원 결제이므로 재고가 즉시 차감되어야 함
        ProductOption updatedOption = productOptionRepository.findById(savedOption.getId()).orElseThrow();
        assertThat(updatedOption.getQuantity()).isEqualTo(99L);
    }

    @Test
    @DisplayName("주문 생성 동시성 테스트 - 여러 사용자가 동시에 주문해도 재고는 정확히 차감된다 (0원 결제)")
    void 주문_생성_동시성_테스트_영원_결제() throws InterruptedException {
        // given
        int threadCount = 10;
        long initialStock = 50L;

        Product product = Product.builder()
                .name("인기 상품")
                .description("재고 한정 상품")
                .basePrice(10000L)
                .build();
        Product savedProduct = productRepository.save(product);

        ProductOption productOption = ProductOption.builder()
                .product(savedProduct)
                .optionName("한정판")
                .price(10000L)
                .quantity(initialStock)
                .build();
        ProductOption savedOption = productOptionRepository.save(productOption);

        Coupon coupon = Coupon.builder()
                .couponName("100% 할인 쿠폰")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(100)
                .totalQuantity(100)
                .issuedQuantity(0)
                .limitPerUser(10)
                .duration(30)
                .minOrderValue(0)
                .validFrom(LocalDate.now().minusDays(1))
                .validUntil(LocalDate.now().plusDays(30))
                .build();
        Coupon savedCoupon = couponRepository.save(coupon);

        // 각 사용자별 UserPoint와 UserCoupon 생성
        List<UserPoint> userPoints = new ArrayList<>();
        List<UserCoupon> userCoupons = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            UserPoint userPoint = UserPoint.builder()
                    .balance(100000L)
                    .build();
            UserPoint savedUserPoint = userPointRepository.save(userPoint);
            userPoints.add(savedUserPoint);

            UserCoupon userCoupon = UserCoupon.builder()
                    .userId(savedUserPoint.getId())
                    .couponId(savedCoupon.getId())
                    .coupon(savedCoupon)
                    .status(Status.ISSUED)
                    .expiredAt(LocalDate.now().plusDays(30))
                    .build();
            UserCoupon savedUserCoupon = userCouponRepository.save(userCoupon);
            userCoupons.add(savedUserCoupon);
        }

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        // when
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executorService.submit(() -> {
                try {
                    startLatch.await();

                    CreateOrderUseCase.Input input = new CreateOrderUseCase.Input(
                            userPoints.get(index).getId(),
                            Arrays.asList(
                                    new CreateOrderUseCase.Input.ProductInfo(savedOption.getId(), 1)
                            ),
                            userCoupons.get(index).getId(),
                            0L
                    );

                    createOrderUseCase.execute(input);
                    successCount.incrementAndGet();

                } catch (Exception e) {
                    log.error("Order creation failed for user {}: {}", index, e.getMessage());
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        endLatch.await();
        executorService.shutdown();

        // then
        assertThat(successCount.get()).isEqualTo(threadCount);

        ProductOption updatedOption = productOptionRepository.findById(savedOption.getId()).orElseThrow();
        assertThat(updatedOption.getQuantity()).isEqualTo(initialStock - threadCount);
    }

    @Test
    @DisplayName("주문 생성 동시성 테스트 - 재고 부족 시 일부만 성공")
    void 주문_생성_동시성_테스트_재고_부족() throws InterruptedException {
        // given
        int threadCount = 20;
        long initialStock = 10L; // 재고를 스레드 수보다 적게 설정

        Product product = Product.builder()
                .name("한정 상품")
                .description("재고 부족 테스트")
                .basePrice(10000L)
                .build();
        Product savedProduct = productRepository.save(product);

        ProductOption productOption = ProductOption.builder()
                .product(savedProduct)
                .optionName("한정판")
                .price(10000L)
                .quantity(initialStock)
                .build();
        ProductOption savedOption = productOptionRepository.save(productOption);

        Coupon coupon = Coupon.builder()
                .couponName("100% 할인 쿠폰")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(100)
                .totalQuantity(100)
                .issuedQuantity(0)
                .limitPerUser(10)
                .duration(30)
                .minOrderValue(0)
                .validFrom(LocalDate.now().minusDays(1))
                .validUntil(LocalDate.now().plusDays(30))
                .build();
        Coupon savedCoupon = couponRepository.save(coupon);

        // 각 사용자별 UserPoint와 UserCoupon 생성
        List<UserPoint> userPoints = new ArrayList<>();
        List<UserCoupon> userCoupons = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            UserPoint userPoint = UserPoint.builder()
                    .balance(100000L)
                    .build();
            UserPoint savedUserPoint = userPointRepository.save(userPoint);
            userPoints.add(savedUserPoint);

            UserCoupon userCoupon = UserCoupon.builder()
                    .userId(savedUserPoint.getId())
                    .couponId(savedCoupon.getId())
                    .coupon(savedCoupon)
                    .status(Status.ISSUED)
                    .expiredAt(LocalDate.now().plusDays(30))
                    .build();
            UserCoupon savedUserCoupon = userCouponRepository.save(userCoupon);
            userCoupons.add(savedUserCoupon);
        }

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executorService.submit(() -> {
                try {
                    startLatch.await();

                    CreateOrderUseCase.Input input = new CreateOrderUseCase.Input(
                            userPoints.get(index).getId(),
                            Arrays.asList(
                                    new CreateOrderUseCase.Input.ProductInfo(savedOption.getId(), 1)
                            ),
                            userCoupons.get(index).getId(),
                            0L
                    );

                    createOrderUseCase.execute(input);
                    successCount.incrementAndGet();

                } catch (Exception e) {
                    log.info("Expected failure for user {}: {}", index, e.getMessage());
                    failCount.incrementAndGet();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        endLatch.await();
        executorService.shutdown();

        // then
        assertThat(successCount.get()).isEqualTo(initialStock);
        assertThat(failCount.get()).isEqualTo(threadCount - initialStock);

        ProductOption updatedOption = productOptionRepository.findById(savedOption.getId()).orElseThrow();
        assertThat(updatedOption.getQuantity()).isEqualTo(0L);
    }
}
