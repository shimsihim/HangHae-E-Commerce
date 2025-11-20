package io.hhplus.tdd.domain.order.application;

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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CancelOrderUseCaseTest {

    @InjectMocks
    CancelOrderUseCase cancelOrderUseCase;

    @Mock
    OrderRepository orderRepository;

    @Mock
    OrderItemRepository orderItemRepository;

    @Mock
    ProductOptionRepository productOptionRepository;

    @Mock
    OrderService orderService;

    @Nested
    class 주문_취소_성공 {

        @Test
        void 정상_주문_취소() {
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

            given(orderRepository.findById(orderId)).willReturn(Optional.of(order));
            given(orderItemRepository.findByOrderId(orderId)).willReturn(Arrays.asList(orderItem));

            CancelOrderUseCase.Input input = new CancelOrderUseCase.Input(orderId);

            // when
            cancelOrderUseCase.execute(input);

            // then
            verify(orderRepository).findById(orderId);
            verify(orderService).cancelOrder(any(Order.class));
            verify(orderItemRepository).findByOrderId(orderId);
            verify(orderService,never()).restoreStock(anyList(), anyList());
        }
    }

    @Nested
    class 주문_취소_실패 {

        @Test
        void 존재하지_않는_주문() {
            // given
            long orderId = 9999L;
            given(orderRepository.findById(orderId)).willReturn(Optional.empty());

            CancelOrderUseCase.Input input = new CancelOrderUseCase.Input(orderId);

            // when & then
            assertThatThrownBy(() -> cancelOrderUseCase.execute(input))
                    .isInstanceOf(OrderException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.ORDER_NOT_FOUND);

            verify(orderRepository).findById(orderId);
            verify(orderService, never()).cancelOrder(any());
            verify(orderService, never()).restoreStock(anyList(), anyList());
        }

        @Test
        void PENDING_상태가_아닌_주문_취소_시도() {
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
            doThrow(new OrderException(ErrorCode.ORDER_CANNOT_CANCEL, orderId))
                    .when(orderService).cancelOrder(order);

            CancelOrderUseCase.Input input = new CancelOrderUseCase.Input(orderId);

            // when & then
            assertThatThrownBy(() -> cancelOrderUseCase.execute(input))
                    .isInstanceOf(OrderException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.ORDER_CANNOT_CANCEL);

            verify(orderRepository).findById(orderId);
            verify(orderService).cancelOrder(order);
            verify(orderService, never()).restoreStock(anyList(), anyList());
        }
    }
}
