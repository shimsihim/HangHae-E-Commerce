package io.hhplus.tdd.domain.order.application;

import io.hhplus.tdd.domain.BaseIntegrationTest;
import io.hhplus.tdd.domain.coupon.domain.model.Coupon;
import io.hhplus.tdd.domain.coupon.domain.model.DiscountType;
import io.hhplus.tdd.domain.coupon.domain.model.Status;
import io.hhplus.tdd.domain.coupon.domain.model.UserCoupon;
import io.hhplus.tdd.domain.order.domain.model.Order;
import io.hhplus.tdd.domain.order.domain.model.OrderItem;
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
class PayCompleteOrderIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private PayCompleteOrderUseCase payCompleteOrderUseCase;

    @Autowired
    private CreateOrderUseCase createOrderUseCase;

    @Autowired
    private UserPointRepository userPointRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductOptionRepository productOptionRepository;

    @Test
    @DisplayName("결제 완료 통합 테스트 - 정상적으로 결제가 완료된다")
    void 결제_완료_처리_성공() {
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

        // 주문 생성
        CreateOrderUseCase.Input createInput = new CreateOrderUseCase.Input(
                savedUserPoint.getId(),
                Arrays.asList(
                        new CreateOrderUseCase.Input.ProductInfo(savedOption.getId(), 2)
                ),
                null,
                5000L
        );
        CreateOrderUseCase.Output createResult = createOrderUseCase.execute(createInput);

        // when
        PayCompleteOrderUseCase.Input payInput = new PayCompleteOrderUseCase.Input(createResult.orderId());
        payCompleteOrderUseCase.execute(payInput);

        // then
        Order order = orderRepository.findById(createResult.orderId()).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(order.getPaidAt()).isNotNull();

        // 재고 차감 확인
        ProductOption updatedOption = productOptionRepository.findById(savedOption.getId()).orElseThrow();
        assertThat(updatedOption.getQuantity()).isEqualTo(98L);

        // 포인트 차감 확인
        UserPoint updatedUserPoint = userPointRepository.findById(savedUserPoint.getId()).orElseThrow();
        assertThat(updatedUserPoint.getBalance()).isEqualTo(95000L);
    }

    @Test
    @DisplayName("결제 완료 통합 테스트 - 쿠폰 사용 결제")
    void 쿠폰_사용_결제_완료() {
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

        // 주문 생성
        CreateOrderUseCase.Input createInput = new CreateOrderUseCase.Input(
                savedUserPoint.getId(),
                Arrays.asList(
                        new CreateOrderUseCase.Input.ProductInfo(savedOption.getId(), 2)
                ),
                savedUserCoupon.getId(),
                0L
        );
        CreateOrderUseCase.Output createResult = createOrderUseCase.execute(createInput);

        // when
        PayCompleteOrderUseCase.Input payInput = new PayCompleteOrderUseCase.Input(createResult.orderId());
        payCompleteOrderUseCase.execute(payInput);

        // then
        Order order = orderRepository.findById(createResult.orderId()).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);

        // 쿠폰 사용 확인
        UserCoupon updatedUserCoupon = userCouponRepository.findById(savedUserCoupon.getId()).orElseThrow();
        assertThat(updatedUserCoupon.getStatus()).isEqualTo(Status.USED);
        assertThat(updatedUserCoupon.getUsedAt()).isNotNull();
    }

    @Test
    @DisplayName("결제 완료 동시성 테스트 - 같은 재고에 대한 동시 결제 처리")
    void 결제_완료_동시성_테스트() throws InterruptedException {
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

        // 각 사용자별 주문 생성
        List<Long> orderIds = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            UserPoint userPoint = UserPoint.builder()
                    .balance(100000L)
                    .build();
            UserPoint savedUserPoint = userPointRepository.save(userPoint);

            CreateOrderUseCase.Input createInput = new CreateOrderUseCase.Input(
                    savedUserPoint.getId(),
                    Arrays.asList(
                            new CreateOrderUseCase.Input.ProductInfo(savedOption.getId(), 1)
                    ),
                    null,
                    0L
            );
            CreateOrderUseCase.Output createResult = createOrderUseCase.execute(createInput);
            orderIds.add(createResult.orderId());
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

                    PayCompleteOrderUseCase.Input payInput = new PayCompleteOrderUseCase.Input(orderIds.get(index));
                    payCompleteOrderUseCase.execute(payInput);
                    successCount.incrementAndGet();

                } catch (Exception e) {
                    log.error("Payment failed for order {}: {}", orderIds.get(index), e.getMessage());
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

        // 모든 주문이 PAID 상태인지 확인
        for (Long orderId : orderIds) {
            Order order = orderRepository.findById(orderId).orElseThrow();
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        }
    }

    @Test
    @DisplayName("결제 완료 동시성 테스트 - 재고 부족 시 일부만 성공")
    void 결제_완료_동시성_테스트_재고_부족() throws InterruptedException {
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

        // 각 사용자별 주문 생성
        List<Long> orderIds = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            UserPoint userPoint = UserPoint.builder()
                    .balance(100000L)
                    .build();
            UserPoint savedUserPoint = userPointRepository.save(userPoint);

            CreateOrderUseCase.Input createInput = new CreateOrderUseCase.Input(
                    savedUserPoint.getId(),
                    Arrays.asList(
                            new CreateOrderUseCase.Input.ProductInfo(savedOption.getId(), 1)
                    ),
                    null,
                    0L
            );
            CreateOrderUseCase.Output createResult = createOrderUseCase.execute(createInput);
            orderIds.add(createResult.orderId());
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

                    PayCompleteOrderUseCase.Input payInput = new PayCompleteOrderUseCase.Input(orderIds.get(index));
                    payCompleteOrderUseCase.execute(payInput);
                    successCount.incrementAndGet();

                } catch (Exception e) {
                    log.info("Expected failure for order {}: {}", orderIds.get(index), e.getMessage());
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

        // 성공한 주문은 PAID, 실패한 주문은 PENDING 상태여야 함
        long paidCount = orderIds.stream()
                .map(orderId -> orderRepository.findById(orderId).orElseThrow())
                .filter(order -> order.getStatus() == OrderStatus.PAID)
                .count();
        assertThat(paidCount).isEqualTo(initialStock);
    }
}
