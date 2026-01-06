package io.hhplus.tdd.domain.statistics.application;

import io.hhplus.tdd.common.consumed.ConsumedEventLog;
import io.hhplus.tdd.common.consumed.ConsumedEventLogRepository;
import io.hhplus.tdd.domain.ContainerIntegrationTest;
import io.hhplus.tdd.domain.IntegrationTest;
import io.hhplus.tdd.domain.coupon.infrastructure.queue.CouponIssueConsumer;
import io.hhplus.tdd.domain.order.application.PayCompleteOrderUseCase;
import io.hhplus.tdd.domain.order.domain.model.Order;
import io.hhplus.tdd.domain.order.domain.model.OrderItem;
import io.hhplus.tdd.domain.order.infrastructure.repository.OrderItemRepository;
import io.hhplus.tdd.domain.order.infrastructure.repository.OrderRepository;
import io.hhplus.tdd.domain.point.application.PointChargeUseCase;
import io.hhplus.tdd.domain.point.domain.model.UserPoint;
import io.hhplus.tdd.domain.point.infrastructure.repository.UserPointRepository;
import io.hhplus.tdd.domain.product.domain.model.Product;
import io.hhplus.tdd.domain.product.domain.model.ProductOption;
import io.hhplus.tdd.domain.product.infrastructure.repository.ProductOptionRepository;
import io.hhplus.tdd.domain.product.infrastructure.repository.ProductRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class OrderStatisticsIntegrationTest extends IntegrationTest {

    @Autowired
    private PayCompleteOrderUseCase payCompleteOrderUseCase;

    @Autowired
    private PointChargeUseCase pointChargeUseCase;

    @Autowired
    private UserPointRepository userPointRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductOptionRepository productOptionRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private ConsumedEventLogRepository consumedEventLogRepository;

    @Autowired
    private io.hhplus.tdd.domain.point.infrastructure.repository.PointHistoryRepository pointHistoryRepository;

    @AfterEach
    void tearDown() {
        // 테스트 데이터 정리 (외래 키 순서 고려)
        consumedEventLogRepository.deleteAllInBatch();
        orderItemRepository.deleteAllInBatch();
        orderRepository.deleteAllInBatch();
        productOptionRepository.deleteAllInBatch();
        productRepository.deleteAllInBatch();
        pointHistoryRepository.deleteAllInBatch();
        userPointRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("주문이 완료되면, 통계 서비스에서 해당 이벤트 로그를 기록하는 것을 검증")
    void should_log_event_when_order_is_completed() {
        // given
        // 1. 유저, 상품, 주문 등 테스트 데이터 준비
        long userId = 1L;
        long chargeAmount = 50000L;
        long productPrice = 10000L;
        int quantity = 2;
        long totalAmount = productPrice * quantity;

        // 유저 생성 및 포인트 충전
        userPointRepository.save(UserPoint.builder().id(userId).balance(0L).version(0L).build());
        pointChargeUseCase.execute(new PointChargeUseCase.Input(userId, chargeAmount, "충전"));
        UserPoint userPoint = userPointRepository.findById(userId).get();

        // 상품 및 옵션 생성
        Product product = productRepository.save(Product.builder().name("TDD Special Edition").basePrice(productPrice).build());
        ProductOption productOption = productOptionRepository.save(
                ProductOption.builder()
                        .product(product)
                        .productId(product.getId())
                        .optionName("Standard")
                        .price(productPrice)
                        .quantity(10L)
                        .build()
        );

        // 주문 생성
        Order order = orderRepository.save(
                Order.createOrder(userPoint, null, totalAmount, 0L, totalAmount)
        );
        orderItemRepository.save(
                OrderItem.builder()
                        .order(order)
                        .orderId(order.getId())
                        .product(product)
                        .productId(product.getId())
                        .productOption(productOption)
                        .productOptionId(productOption.getId())
                        .quantity(quantity)
                        .unitPrice(productOption.getPrice())
                        .subtotal(productOption.getPrice() * quantity)
                        .build()
        );
        Long orderId = order.getId();

        // when
        // 2. 주문 완료 처리
        payCompleteOrderUseCase.execute(new PayCompleteOrderUseCase.Input(orderId));

        // then
        // 3. 통계 서비스가 이벤트를 소비하고 로그를 남길 때까지 대기
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            boolean isConsumed = consumedEventLogRepository.existsByEventIdAndEventType(
                    String.valueOf(orderId),
                    "OrderCompleted"
            );
            assertThat(isConsumed).isTrue();
        });

        // 4. 로그 상세 검증
        Optional<ConsumedEventLog> logOpt = consumedEventLogRepository.findByEventIdAndEventType(String.valueOf(orderId), "OrderCompleted");
        assertThat(logOpt).isPresent();
        ConsumedEventLog log = logOpt.get();
        assertThat(log.getEventType()).isEqualTo("OrderCompleted");
        assertThat(log.getConsumerName()).isEqualTo("OrderCompletedEventConsumer");
        assertThat(log.getPayload()).contains("\"orderId\":" + orderId);
    }
}
