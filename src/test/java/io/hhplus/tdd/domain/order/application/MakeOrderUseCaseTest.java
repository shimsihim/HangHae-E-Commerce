package io.hhplus.tdd.domain.order.application;

import io.hhplus.tdd.common.exception.ErrorCode;
import io.hhplus.tdd.domain.coupon.domain.model.Coupon;
import io.hhplus.tdd.domain.coupon.domain.model.DiscountType;
import io.hhplus.tdd.domain.coupon.domain.model.Status;
import io.hhplus.tdd.domain.coupon.domain.model.UserCoupon;
import io.hhplus.tdd.domain.coupon.domain.repository.CouponRepository;
import io.hhplus.tdd.domain.coupon.domain.repository.UserCouponRepository;
import io.hhplus.tdd.domain.coupon.domain.service.CouponService;
import io.hhplus.tdd.domain.order.domain.model.Order;
import io.hhplus.tdd.domain.order.domain.model.OrderStatus;
import io.hhplus.tdd.domain.order.domain.repository.OrderItemRepository;
import io.hhplus.tdd.domain.order.domain.repository.OrderRepository;
import io.hhplus.tdd.domain.order.domain.service.OrderService;
import io.hhplus.tdd.domain.point.domain.model.PointHistory;
import io.hhplus.tdd.domain.point.domain.model.UserPoint;
import io.hhplus.tdd.domain.point.domain.repository.PointHistoryRepository;
import io.hhplus.tdd.domain.point.domain.repository.UserPointRepository;
import io.hhplus.tdd.domain.point.domain.service.PointService;
import io.hhplus.tdd.domain.point.exception.PointRangeException;
import io.hhplus.tdd.domain.product.domain.model.ProductOption;
import io.hhplus.tdd.domain.product.domain.repository.ProductOptionRepository;
import io.hhplus.tdd.domain.product.exception.ProductException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MakeOrderUseCaseTest {

    @InjectMocks
    MakeOrderUseCase makeOrderUseCase;

    @Mock
    OrderRepository orderRepository;

    @Mock
    OrderItemRepository orderItemRepository;

    @Mock
    ProductOptionRepository productOptionRepository;

    @Mock
    CouponRepository couponRepository;

    @Mock
    UserCouponRepository userCouponRepository;

    @Mock
    UserPointRepository userPointRepository;

    @Mock
    PointHistoryRepository pointHistoryRepository;

    @Mock
    CouponService couponService;

    @Mock
    PointService pointService;

    @Mock
    OrderService orderService;

    @Nested
    class 주문_성공 {

        @Test
        void 쿠폰_사용_주문() {
            // given
            long userId = 1L;
            long productOptionId = 1L;
            long userCouponId = 1L;
            long quantity = 2L;
            long price = 10000L;
            long totalAmount = price * quantity;
            long discountAmount = 2000L;

            ProductOption productOption = ProductOption.builder()
                    .id(productOptionId)
                    .productId(1L)
                    .price(price)
                    .quantity(10L)
                    .build();

            UserPoint userPoint = UserPoint.builder()
                    .id(userId)
                    .balance(50000L)
                    .version(0L)
                    .build();

            Coupon coupon = Coupon.builder()
                    .id(1L)
                    .discountType(DiscountType.FIXED_AMOUNT)
                    .discountValue(2000)
                    .minOrderValue(5000)
                    .validFrom(LocalDate.now().minusDays(1))
                    .validUntil(LocalDate.now().plusDays(30))
                    .build();

            UserCoupon userCoupon = UserCoupon.builder()
                    .id(userCouponId)
                    .userId(userId)
                    .couponId(1L)
                    .status(Status.ISSUED)
                    .expiredAt(LocalDate.now().plusDays(30))
                    .build();

            Order order = Order.builder()
                    .id(1L)
                    .userId(userId)
                    .totalAmount(totalAmount)
                    .discountAmount(discountAmount)
                    .usePointAmount(totalAmount - discountAmount)
                    .finalAmount(0L)
                    .status(OrderStatus.PENDING)
                    .build();

            given(productOptionRepository.findById(productOptionId)).willReturn(Optional.of(productOption));
            given(orderService.calculateTotalAmount(any())).willReturn(totalAmount);
            given(userCouponRepository.findByUserIdAndUserCouponIdAndStatus(userId, userCouponId, Status.ISSUED.toString()))
                    .willReturn(Optional.of(userCoupon));
            given(couponRepository.findById(1L)).willReturn(Optional.of(coupon));
            given(couponService.applyDiscount(coupon, userCoupon, totalAmount)).willReturn(discountAmount);
            given(userPointRepository.findByUserId(userId)).willReturn(Optional.of(userPoint));
            given(orderRepository.save(any(Order.class))).willReturn(order);
            given(productOptionRepository.save(any(ProductOption.class))).willReturn(productOption);
            given(userCouponRepository.save(any(UserCoupon.class))).willReturn(userCoupon);
            given(orderItemRepository.save(any())).willReturn(null);

            // when
            MakeOrderUseCase.Input input = MakeOrderUseCase.Input.of(
                    userId,
                    List.of(MakeOrderUseCase.Input.ItemInfo.of(1L, productOptionId, quantity)),
                    userCouponId,
                    null
            );
            MakeOrderUseCase.Output output = makeOrderUseCase.execute(input);

            // then
            assertThat(output.discountAmount()).isEqualTo(discountAmount);
            verify(couponService).applyDiscount(coupon, userCoupon, totalAmount);
            verify(userCouponRepository).save(userCoupon);
        }

        @Test
        void 포인트_사용_주문() {
            // given
            long userId = 1L;
            long productOptionId = 1L;
            long quantity = 2L;
            long price = 10000L;
            long totalAmount = price * quantity;
            long usePointAmount = 5000L;

            ProductOption productOption = ProductOption.builder()
                    .id(productOptionId)
                    .productId(1L)
                    .price(price)
                    .quantity(10L)
                    .build();

            UserPoint userPoint = UserPoint.builder()
                    .id(userId)
                    .balance(50000L)
                    .version(0L)
                    .build();

            PointHistory pointHistory = PointHistory.createForUse(userId, usePointAmount, 45000L, "주문 결제");

            Order order = Order.builder()
                    .id(1L)
                    .userId(userId)
                    .totalAmount(totalAmount)
                    .discountAmount(0L)
                    .usePointAmount(usePointAmount)
                    .finalAmount(totalAmount - usePointAmount)
                    .status(OrderStatus.PENDING)
                    .build();

            given(productOptionRepository.findById(productOptionId)).willReturn(Optional.of(productOption));
            given(orderService.calculateTotalAmount(any())).willReturn(totalAmount);
            given(userPointRepository.findByUserId(userId)).willReturn(Optional.of(userPoint));
            given(pointService.usePoint(any(), eq(usePointAmount), anyString())).willReturn(pointHistory);
            given(orderRepository.save(any(Order.class))).willReturn(order);
            given(productOptionRepository.save(any(ProductOption.class))).willReturn(productOption);
            given(userPointRepository.save(any(UserPoint.class))).willReturn(userPoint);
            given(pointHistoryRepository.save(any(PointHistory.class))).willReturn(pointHistory);
            given(orderItemRepository.save(any())).willReturn(null);

            // when
            MakeOrderUseCase.Input input = MakeOrderUseCase.Input.of(
                    userId,
                    List.of(MakeOrderUseCase.Input.ItemInfo.of(1L, productOptionId, quantity)),
                    null,
                    usePointAmount
            );
            MakeOrderUseCase.Output output = makeOrderUseCase.execute(input);

            // then
            assertThat(output.usePointAmount()).isEqualTo(usePointAmount);
            verify(pointService).usePoint(any(), eq(usePointAmount), anyString());
            verify(pointHistoryRepository).save(pointHistory);
        }
    }

    @Nested
    class OrderFailure {

        @Test
        void 상품_재고_부족() {
            // given
            long userId = 1L;
            long productOptionId = 1L;
            long quantity = 10L;

            ProductOption productOption = ProductOption.builder()
                    .id(productOptionId)
                    .productId(1L)
                    .price(10000L)
                    .quantity(5L)  // 재고 부족
                    .build();

            given(productOptionRepository.findById(productOptionId)).willReturn(Optional.of(productOption));
            given(orderService.calculateTotalAmount(any())).willReturn(100000L);
            doThrow(new ProductException(ErrorCode.PRODUCT_NOT_ENOUGH, 1L, productOptionId))
                    .when(orderService).validateAndDeductStock(any(), any());

            // when , then
            MakeOrderUseCase.Input input = MakeOrderUseCase.Input.of(
                    userId,
                    List.of(MakeOrderUseCase.Input.ItemInfo.of(1L, productOptionId, quantity)),
                    null,
                    null
            );

            assertThatThrownBy(() -> makeOrderUseCase.execute(input))
                    .isInstanceOf(ProductException.class);
        }

        @Test
        void 포인트_잔액_부족() {
            // given
            long userId = 1L;
            long productOptionId = 1L;
            long quantity = 2L;
            long usePointAmount = 60000L;  // 잔액보다 많음

            ProductOption productOption = ProductOption.builder()
                    .id(productOptionId)
                    .productId(1L)
                    .price(10000L)
                    .quantity(10L)
                    .build();

            UserPoint userPoint = UserPoint.builder()
                    .id(userId)
                    .balance(usePointAmount - 10000)  // 잔액 부족
                    .version(0L)
                    .build();

            given(productOptionRepository.findById(productOptionId)).willReturn(Optional.of(productOption));
            given(orderService.calculateTotalAmount(any())).willReturn(20000L);
            given(userPointRepository.findByUserId(userId)).willReturn(Optional.of(userPoint));

            // when , then
            MakeOrderUseCase.Input input = MakeOrderUseCase.Input.of(
                    userId,
                    List.of(MakeOrderUseCase.Input.ItemInfo.of(1L, productOptionId, quantity)),
                    null,
                    usePointAmount
            );

            assertThatThrownBy(() -> makeOrderUseCase.execute(input))
                    .isInstanceOf(PointRangeException.class);
        }
    }

    @Nested
    class OrderRollback {

        @Test
        void 포인트_히스토리_저장_실패_롤백() {
            // given
            long userId = 1L;
            long productOptionId = 1L;
            long quantity = 2L;
            long price = 10000L;
            long usePointAmount = 5000L;
            long originalStock = 10L;

            ProductOption productOption = ProductOption.builder()
                    .id(productOptionId)
                    .productId(1L)
                    .price(price)
                    .quantity(originalStock)
                    .build();

            UserPoint userPoint = UserPoint.builder()
                    .id(userId)
                    .balance(50000L)
                    .version(0L)
                    .build();

            PointHistory pointHistory = PointHistory.createForUse(userId, usePointAmount, 45000L, "주문 결제");

            Order order = Order.builder()
                    .id(1L)
                    .userId(userId)
                    .status(OrderStatus.PENDING)
                    .build();

            given(productOptionRepository.findById(productOptionId)).willReturn(Optional.of(productOption));
            given(orderService.calculateTotalAmount(any())).willReturn(price * quantity);
            given(userPointRepository.findByUserId(userId)).willReturn(Optional.of(userPoint));
            given(orderRepository.save(any(Order.class))).willReturn(order);
            given(productOptionRepository.save(any(ProductOption.class))).willReturn(productOption);
            given(userPointRepository.save(any(UserPoint.class))).willReturn(userPoint);
            given(pointService.usePoint(any(), eq(usePointAmount), anyString())).willReturn(pointHistory);
            given(pointHistoryRepository.save(any(PointHistory.class)))
                    .willThrow(new RuntimeException(""));

            // when , then
            MakeOrderUseCase.Input input = MakeOrderUseCase.Input.of(
                    userId,
                    List.of(MakeOrderUseCase.Input.ItemInfo.of(1L, productOptionId, quantity)),
                    null,
                    usePointAmount
            );

            // 롤백 검증
            verify(orderService).restoreStock(any(), any(), any());
            verify(userPointRepository, times(2)).save(any(UserPoint.class));
        }
    }
}
