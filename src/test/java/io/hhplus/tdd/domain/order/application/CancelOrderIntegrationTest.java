package io.hhplus.tdd.domain.order.application;

import io.hhplus.tdd.domain.IntegrationTest;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
class CancelOrderIntegrationTest extends IntegrationTest {

    @Autowired
    private CancelOrderUseCase cancelOrderUseCase;

    @Autowired
    private CreateOrderUseCase createOrderUseCase;

    @Autowired
    private UserPointRepository userPointRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductOptionRepository productOptionRepository;

    @Test
    @DisplayName("주문 취소 통합 테스트 - 정상적으로 주문이 취소된다")
    void 주문_취소_성공() {
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
                0L
        );
        CreateOrderUseCase.Output createResult = createOrderUseCase.execute(createInput);

        // when
        CancelOrderUseCase.Input cancelInput = new CancelOrderUseCase.Input(createResult.orderId());
        cancelOrderUseCase.execute(cancelInput);

        // then
        Order order = orderRepository.findById(createResult.orderId()).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);

        // 재고 복구 확인 (주문 시 재고가 차감되지 않았으므로 그대로 100)
        ProductOption updatedOption = productOptionRepository.findById(savedOption.getId()).orElseThrow();
        assertThat(updatedOption.getQuantity()).isEqualTo(100L);
    }


    @Test
    @DisplayName("주문 취소 동시성 테스트 - 여러 주문을 동시에 취소해도 재고는 정확히 복구된다")
    void 주문_취소_동시성_테스트() throws InterruptedException {
        // given
        int threadCount = 10;
        long initialStock = 100L;

        Product product = Product.builder()
                .name("테스트 상품")
                .description("재고 복구 테스트")
                .basePrice(10000L)
                .build();
        Product savedProduct = productRepository.save(product);

        ProductOption productOption = ProductOption.builder()
                .product(savedProduct)
                .optionName("기본 옵션")
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

                    CancelOrderUseCase.Input cancelInput = new CancelOrderUseCase.Input(orderIds.get(index));
                    cancelOrderUseCase.execute(cancelInput);
                    successCount.incrementAndGet();

                } catch (Exception e) {
                    log.error("Cancel failed for order {}: {}", orderIds.get(index), e.getMessage());
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

        // 재고는 그대로 유지 (주문 생성 시 재고가 차감되지 않음)
        ProductOption updatedOption = productOptionRepository.findById(savedOption.getId()).orElseThrow();
        assertThat(updatedOption.getQuantity()).isEqualTo(initialStock);

        // 모든 주문이 CANCELLED 상태인지 확인
        for (Long orderId : orderIds) {
            Order order = orderRepository.findById(orderId).orElseThrow();
            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        }
    }
}
