package io.hhplus.tdd.domain.order.application;

import io.hhplus.tdd.common.distributedLock.MultiDistributedLockExecutor;
import io.hhplus.tdd.common.exception.ErrorCode;
import io.hhplus.tdd.domain.order.domain.model.Order;
import io.hhplus.tdd.domain.order.domain.model.OrderItem;
import io.hhplus.tdd.domain.order.domain.model.OrderStatus;
import io.hhplus.tdd.domain.order.domain.service.OrderService;
import io.hhplus.tdd.domain.order.exception.OrderException;
import io.hhplus.tdd.domain.order.infrastructure.repository.OrderItemRepository;
import io.hhplus.tdd.domain.order.infrastructure.repository.OrderRepository;
import io.hhplus.tdd.domain.point.domain.model.UserPoint;
import io.hhplus.tdd.domain.product.domain.model.Product;
import io.hhplus.tdd.domain.product.domain.model.ProductOption;
import io.hhplus.tdd.domain.product.infrastructure.repository.ProductOptionRepository;
import io.hhplus.tdd.domain.product.infrastructure.repository.ProductRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PayCompleteOrderUseCaseTest {

    @InjectMocks
    PayCompleteOrderUseCase payCompleteOrderUseCase;

    @Mock
    OrderRepository orderRepository;

    @Mock
    OrderItemRepository orderItemRepository;

    @Mock
    ProductOptionRepository productOptionRepository;

    @Mock
    ProductRepository productRepository;

    @Mock
    OrderService orderService;

    @Mock
    MultiDistributedLockExecutor lockExecutor;

    @Mock
    TransactionTemplate transactionTemplate;

    @Mock
    OrderEventPublisher orderEventPublisher;

    @Nested
    class 결제_완료_처리_성공 {

        @Test
        void 정상_결제_완료_처리() {
            // given
            long orderId = 1L;
            long userId = 1L;
            long productId = 100L;
            long optionId = 200L;

            UserPoint userPoint = UserPoint.builder()
                    .id(userId)
                    .balance(100000L)
                    .build();

            Product product = Product.builder()
                    .id(productId)
                    .name("테스트 상품")
                    .basePrice(10000L)
                    .build();

            ProductOption productOption = ProductOption.builder()
                    .id(optionId)
                    .product(product)
                    .productId(productId)
                    .optionName("기본")
                    .price(10000L)
                    .quantity(100L)
                    .build();

            Order order = Order.builder()
                    .id(orderId)
                    .userId(userId)
                    .userPoint(userPoint)
                    .userCoupon(null)
                    .status(OrderStatus.PENDING)
                    .totalAmount(20000L)
                    .discountAmount(0L)
                    .usePointAmount(0L)
                    .finalAmount(20000L)
                    .build();

            OrderItem orderItem = OrderItem.builder()
                    .id(1L)
                    .order(order)
                    .orderId(orderId)
                    .product(product)
                    .productId(productId)
                    .productOption(productOption)
                    .productOptionId(optionId)
                    .quantity(2)
                    .unitPrice(10000L)
                    .subtotal(20000L)
                    .build();

            // Mock 설정: lockExecutor는 전달된 Runnable을 바로 실행
            willAnswer(invocation -> {
                Runnable task = invocation.getArgument(1);
                task.run();
                return null;
            }).given(lockExecutor).executeWithLocks(anyList(), any(Runnable.class));

            // Mock 설정: transactionTemplate은 전달된 콜백을 바로 실행하고 Order를 반환
            willAnswer(invocation -> {
                org.springframework.transaction.support.TransactionCallback<?> callback = invocation.getArgument(0);
                return callback.doInTransaction(null);
            }).given(transactionTemplate).execute(any());

            given(orderRepository.findById(orderId)).willReturn(Optional.of(order));
            given(orderItemRepository.findByOrderId(orderId)).willReturn(Arrays.asList(orderItem));
            given(productOptionRepository.findAllByIdInForUpdate(anyList()))
                    .willReturn(Arrays.asList(productOption));

            PayCompleteOrderUseCase.Input input = new PayCompleteOrderUseCase.Input(orderId);

            // when
            payCompleteOrderUseCase.execute(input);

            // then
            verify(orderRepository, atLeastOnce()).findById(orderId);
            verify(orderItemRepository).findByOrderId(orderId);
            verify(productOptionRepository).findAllByIdInForUpdate(anyList());
            verify(orderService).completeOrderWithPayment(any(Order.class), anyList(), anyList());
            verify(orderEventPublisher).publishOrderCompletedEvent(any(Order.class));
        }
    }

    @Nested
    class 결제_완료_처리_실패 {

        @Test
        void 존재하지_않는_주문() {
            // given
            long orderId = 9999L;
            given(orderRepository.findById(orderId)).willReturn(Optional.empty());

            PayCompleteOrderUseCase.Input input = new PayCompleteOrderUseCase.Input(orderId);

            // when & then
            assertThatThrownBy(() -> payCompleteOrderUseCase.execute(input))
                    .isInstanceOf(OrderException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.ORDER_NOT_FOUND);

            verify(orderRepository).findById(orderId);
            verify(orderService, never()).completeOrderWithPayment(any(), anyList(), anyList());
        }

        @Test
        void PENDING_상태가_아닌_주문() {
            // given
            long orderId = 1L;
            long userId = 1L;

            UserPoint userPoint = UserPoint.builder()
                    .id(userId)
                    .balance(100000L)
                    .build();

            Order order = Order.builder()
                    .id(orderId)
                    .userId(userId)
                    .userPoint(userPoint)
                    .userCoupon(null)
                    .status(OrderStatus.PAID) // 이미 결제 완료된 상태
                    .totalAmount(20000L)
                    .discountAmount(0L)
                    .usePointAmount(0L)
                    .finalAmount(20000L)
                    .build();

            given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

            PayCompleteOrderUseCase.Input input = new PayCompleteOrderUseCase.Input(orderId);

            // when & then
            assertThatThrownBy(() -> payCompleteOrderUseCase.execute(input))
                    .isInstanceOf(OrderException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.ORDER_NOT_VALID);

            verify(orderRepository).findById(orderId);
            verify(orderService, never()).completeOrderWithPayment(any(), anyList(), anyList());
        }

        @Test
        void 주문_항목이_없는_경우() {
            // given
            long orderId = 1L;
            long userId = 1L;

            UserPoint userPoint = UserPoint.builder()
                    .id(userId)
                    .balance(100000L)
                    .build();

            Order order = Order.builder()
                    .id(orderId)
                    .userId(userId)
                    .userPoint(userPoint)
                    .userCoupon(null)
                    .status(OrderStatus.PENDING)
                    .totalAmount(20000L)
                    .discountAmount(0L)
                    .usePointAmount(0L)
                    .finalAmount(20000L)
                    .build();

            given(orderRepository.findById(orderId)).willReturn(Optional.of(order));
            given(orderItemRepository.findByOrderId(orderId)).willReturn(Collections.emptyList());

            PayCompleteOrderUseCase.Input input = new PayCompleteOrderUseCase.Input(orderId);

            // when & then
            assertThatThrownBy(() -> payCompleteOrderUseCase.execute(input))
                    .isInstanceOf(OrderException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.ORDER_NOT_VALID);

            verify(orderRepository).findById(orderId);
            verify(orderItemRepository).findByOrderId(orderId);
            verify(orderService, never()).completeOrderWithPayment(any(), anyList(), anyList());
        }
    }
}
