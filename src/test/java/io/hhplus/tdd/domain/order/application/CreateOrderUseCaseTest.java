package io.hhplus.tdd.domain.order.application;

import io.hhplus.tdd.common.exception.ErrorCode;
import io.hhplus.tdd.common.exception.UserNotFoundException;
import io.hhplus.tdd.domain.coupon.domain.model.Coupon;
import io.hhplus.tdd.domain.coupon.domain.model.DiscountType;
import io.hhplus.tdd.domain.coupon.domain.model.Status;
import io.hhplus.tdd.domain.coupon.domain.model.UserCoupon;
import io.hhplus.tdd.domain.coupon.domain.service.CouponService;
import io.hhplus.tdd.domain.coupon.exception.CouponException;
import io.hhplus.tdd.domain.coupon.infrastructure.repository.UserCouponRepository;
import io.hhplus.tdd.domain.order.domain.model.Order;
import io.hhplus.tdd.domain.order.domain.model.OrderStatus;
import io.hhplus.tdd.domain.order.domain.service.OrderService;
import io.hhplus.tdd.domain.order.infrastructure.repository.OrderItemRepository;
import io.hhplus.tdd.domain.order.infrastructure.repository.OrderRepository;
import io.hhplus.tdd.domain.point.domain.model.UserPoint;
import io.hhplus.tdd.domain.point.infrastructure.repository.UserPointRepository;
import io.hhplus.tdd.domain.product.domain.model.Product;
import io.hhplus.tdd.domain.product.domain.model.ProductOption;
import io.hhplus.tdd.domain.product.infrastructure.repository.ProductOptionRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateOrderUseCaseTest {

    @InjectMocks
    CreateOrderUseCase createOrderUseCase;

    @Mock
    OrderRepository orderRepository;

    @Mock
    OrderItemRepository orderItemRepository;

    @Mock
    UserPointRepository userPointRepository;

    @Mock
    UserCouponRepository userCouponRepository;

    @Mock
    ProductOptionRepository productOptionRepository;

    @Mock
    CouponService couponService;

    @Mock
    OrderService orderService;

    @Nested
    class 주문_생성_성공 {

        @Test
        void 정상_주문_생성_쿠폰_포인트_미사용() {
            // given
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

            Order savedOrder = Order.builder()
                    .id(1L)
                    .userId(userId)
                    .userPoint(userPoint)
                    .userCoupon(null)
                    .status(OrderStatus.PENDING)
                    .totalAmount(20000L)
                    .discountAmount(0L)
                    .usePointAmount(0L)
                    .finalAmount(20000L)
                    .build();

            given(userPointRepository.findById(userId)).willReturn(Optional.of(userPoint));
            given(productOptionRepository.findAllWithProductByIdIn(anyList()))
                    .willReturn(Arrays.asList(productOption));
            given(orderService.validOrder(anyList(), anyList())).willReturn(20000L);
            given(orderRepository.save(any(Order.class))).willReturn(savedOrder);

            CreateOrderUseCase.Input input = new CreateOrderUseCase.Input(
                    userId,
                    Arrays.asList(
                            new CreateOrderUseCase.Input.ProductInfo(optionId, 2)
                    ),
                    null,
                    0L
            );

            // when
            CreateOrderUseCase.Output result = createOrderUseCase.execute(input);

            // then
            assertThat(result.orderId()).isEqualTo(1L);
            assertThat(result.userId()).isEqualTo(userId);
            assertThat(result.totalAmount()).isEqualTo(20000L);
            assertThat(result.discountAmount()).isEqualTo(0L);
            assertThat(result.usePointAmount()).isEqualTo(0L);
            assertThat(result.finalAmount()).isEqualTo(20000L);

            verify(userPointRepository).findById(userId);
            verify(productOptionRepository).findAllWithProductByIdIn(anyList());
            verify(orderService).validOrder(anyList(), anyList());
            verify(orderRepository).save(any(Order.class));
            verify(orderItemRepository).saveAll(anyList());
        }

        @Test
        void 쿠폰_적용_주문_생성() {
            // given
            long userId = 1L;
            long optionId = 200L;
            long userCouponId = 300L;

            UserPoint userPoint = UserPoint.builder()
                    .id(userId)
                    .balance(100000L)
                    .build();

            Product product = Product.builder()
                    .id(100L)
                    .name("테스트 상품")
                    .basePrice(10000L)
                    .build();

            ProductOption productOption = ProductOption.builder()
                    .id(optionId)
                    .product(product)
                    .productId(100L)
                    .optionName("기본")
                    .price(10000L)
                    .quantity(100L)
                    .build();

            Coupon coupon = Coupon.builder()
                    .id(1L)
                    .couponName("할인 쿠폰")
                    .discountType(DiscountType.FIXED_AMOUNT)
                    .discountValue(5000)
                    .totalQuantity(100)
                    .issuedQuantity(50)
                    .limitPerUser(1)
                    .duration(30)
                    .minOrderValue(10000)
                    .validFrom(LocalDate.now().minusDays(1))
                    .validUntil(LocalDate.now().plusDays(30))
                    .build();

            UserCoupon userCoupon = UserCoupon.builder()
                    .id(userCouponId)
                    .userId(userId)
                    .couponId(coupon.getId())
                    .coupon(coupon)
                    .status(Status.ISSUED)
                    .expiredAt(LocalDate.now().plusDays(30))
                    .build();

            Order savedOrder = Order.builder()
                    .id(1L)
                    .userId(userId)
                    .userPoint(userPoint)
                    .userCoupon(userCoupon)
                    .status(OrderStatus.PENDING)
                    .totalAmount(20000L)
                    .discountAmount(5000L)
                    .usePointAmount(0L)
                    .finalAmount(15000L)
                    .build();

            given(userPointRepository.findById(userId)).willReturn(Optional.of(userPoint));
            given(productOptionRepository.findAllWithProductByIdIn(anyList()))
                    .willReturn(Arrays.asList(productOption));
            given(orderService.validOrder(anyList(), anyList())).willReturn(20000L);
            given(userCouponRepository.findByIdWithCoupon(userCouponId))
                    .willReturn(Optional.of(userCoupon));
            given(couponService.validateCouponUsage(any(Coupon.class), any(UserCoupon.class), anyLong()))
                    .willReturn(5000L);
            given(orderRepository.save(any(Order.class))).willReturn(savedOrder);

            CreateOrderUseCase.Input input = new CreateOrderUseCase.Input(
                    userId,
                    Arrays.asList(
                            new CreateOrderUseCase.Input.ProductInfo(optionId, 2)
                    ),
                    userCouponId,
                    0L
            );

            // when
            CreateOrderUseCase.Output result = createOrderUseCase.execute(input);

            // then
            assertThat(result.orderId()).isEqualTo(1L);
            assertThat(result.totalAmount()).isEqualTo(20000L);
            assertThat(result.discountAmount()).isEqualTo(5000L);
            assertThat(result.finalAmount()).isEqualTo(15000L);

            verify(userCouponRepository).findByIdWithCoupon(userCouponId);
            verify(couponService).validateCouponUsage(any(Coupon.class), any(UserCoupon.class), anyLong());
        }

        @Test
        void 포인트_사용_주문_생성() {
            // given
            long userId = 1L;
            long optionId = 200L;
            long usePointAmount = 5000L;

            UserPoint userPoint = UserPoint.builder()
                    .id(userId)
                    .balance(100000L)
                    .build();

            Product product = Product.builder()
                    .id(100L)
                    .name("테스트 상품")
                    .basePrice(10000L)
                    .build();

            ProductOption productOption = ProductOption.builder()
                    .id(optionId)
                    .product(product)
                    .productId(100L)
                    .optionName("기본")
                    .price(10000L)
                    .quantity(100L)
                    .build();

            Order savedOrder = Order.builder()
                    .id(1L)
                    .userId(userId)
                    .userPoint(userPoint)
                    .userCoupon(null)
                    .status(OrderStatus.PENDING)
                    .totalAmount(20000L)
                    .discountAmount(0L)
                    .usePointAmount(usePointAmount)
                    .finalAmount(15000L)
                    .build();

            given(userPointRepository.findById(userId)).willReturn(Optional.of(userPoint));
            given(productOptionRepository.findAllWithProductByIdIn(anyList()))
                    .willReturn(Arrays.asList(productOption));
            given(orderService.validOrder(anyList(), anyList())).willReturn(20000L);
            given(orderRepository.save(any(Order.class))).willReturn(savedOrder);

            CreateOrderUseCase.Input input = new CreateOrderUseCase.Input(
                    userId,
                    Arrays.asList(
                            new CreateOrderUseCase.Input.ProductInfo(optionId, 2)
                    ),
                    null,
                    usePointAmount
            );

            // when
            CreateOrderUseCase.Output result = createOrderUseCase.execute(input);

            // then
            assertThat(result.orderId()).isEqualTo(1L);
            assertThat(result.totalAmount()).isEqualTo(20000L);
            assertThat(result.usePointAmount()).isEqualTo(usePointAmount);
            assertThat(result.finalAmount()).isEqualTo(15000L);
        }

        @Test
        void 영원_결제_시_즉시_완료_처리() {
            // given
            long userId = 1L;
            long optionId = 200L;

            UserPoint userPoint = UserPoint.builder()
                    .id(userId)
                    .balance(100000L)
                    .build();

            Product product = Product.builder()
                    .id(100L)
                    .name("테스트 상품")
                    .basePrice(10000L)
                    .build();

            ProductOption productOption = ProductOption.builder()
                    .id(optionId)
                    .product(product)
                    .productId(100L)
                    .optionName("기본")
                    .price(10000L)
                    .quantity(100L)
                    .build();

            Coupon coupon = Coupon.builder()
                    .id(1L)
                    .couponName("100% 할인 쿠폰")
                    .discountType(DiscountType.PERCENTAGE)
                    .discountValue(100)
                    .totalQuantity(100)
                    .issuedQuantity(50)
                    .limitPerUser(1)
                    .duration(30)
                    .minOrderValue(0)
                    .validFrom(LocalDate.now().minusDays(1))
                    .validUntil(LocalDate.now().plusDays(30))
                    .build();

            UserCoupon userCoupon = UserCoupon.builder()
                    .id(300L)
                    .userId(userId)
                    .couponId(coupon.getId())
                    .coupon(coupon)
                    .status(Status.ISSUED)
                    .expiredAt(LocalDate.now().plusDays(30))
                    .build();

            Order savedOrder = Order.builder()
                    .id(1L)
                    .userId(userId)
                    .userPoint(userPoint)
                    .userCoupon(userCoupon)
                    .status(OrderStatus.PAID) // 0원 결제 시 즉시 완료
                    .totalAmount(10000L)
                    .discountAmount(10000L)
                    .usePointAmount(0L)
                    .finalAmount(0L)
                    .build();

            given(userPointRepository.findById(userId)).willReturn(Optional.of(userPoint));
            given(productOptionRepository.findAllWithProductByIdIn(anyList()))
                    .willReturn(Arrays.asList(productOption));
            given(orderService.validOrder(anyList(), anyList())).willReturn(10000L);
            given(userCouponRepository.findByIdWithCoupon(300L))
                    .willReturn(Optional.of(userCoupon));
            given(couponService.validateCouponUsage(any(Coupon.class), any(UserCoupon.class), anyLong()))
                    .willReturn(10000L);
            given(productOptionRepository.findAllByIdInForUpdate(anyList()))
                    .willReturn(Arrays.asList(productOption));
            given(orderRepository.save(any(Order.class))).willReturn(savedOrder);

            CreateOrderUseCase.Input input = new CreateOrderUseCase.Input(
                    userId,
                    Arrays.asList(
                            new CreateOrderUseCase.Input.ProductInfo(optionId, 1)
                    ),
                    300L,
                    0L
            );

            // when
            CreateOrderUseCase.Output result = createOrderUseCase.execute(input);

            // then
            assertThat(result.finalAmount()).isEqualTo(0L);
            verify(orderService).completeOrderWithPayment(any(Order.class), anyList(), anyList());
        }
    }

    @Nested
    class 주문_생성_실패 {

        @Test
        void 존재하지_않는_사용자() {
            // given
            long userId = 9999L;
            given(userPointRepository.findById(userId)).willReturn(Optional.empty());

            CreateOrderUseCase.Input input = new CreateOrderUseCase.Input(
                    userId,
                    Arrays.asList(
                            new CreateOrderUseCase.Input.ProductInfo(200L, 1)
                    ),
                    null,
                    0L
            );

            // when & then
            assertThatThrownBy(() -> createOrderUseCase.execute(input))
                    .isInstanceOf(UserNotFoundException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.USER_NOT_FOUND);

            verify(userPointRepository).findById(userId);
            verify(orderRepository, never()).save(any());
        }

        @Test
        void 존재하지_않는_쿠폰() {
            // given
            long userId = 1L;
            long userCouponId = 9999L;

            UserPoint userPoint = UserPoint.builder()
                    .id(userId)
                    .balance(100000L)
                    .build();

            Product product = Product.builder()
                    .id(100L)
                    .name("테스트 상품")
                    .basePrice(10000L)
                    .build();

            ProductOption productOption = ProductOption.builder()
                    .id(200L)
                    .product(product)
                    .productId(100L)
                    .optionName("기본")
                    .price(10000L)
                    .quantity(100L)
                    .build();

            given(userPointRepository.findById(userId)).willReturn(Optional.of(userPoint));
            given(productOptionRepository.findAllWithProductByIdIn(anyList()))
                    .willReturn(Arrays.asList(productOption));
            given(orderService.validOrder(anyList(), anyList())).willReturn(10000L);
            given(userCouponRepository.findByIdWithCoupon(userCouponId))
                    .willReturn(Optional.empty());

            CreateOrderUseCase.Input input = new CreateOrderUseCase.Input(
                    userId,
                    Arrays.asList(
                            new CreateOrderUseCase.Input.ProductInfo(200L, 1)
                    ),
                    userCouponId,
                    0L
            );

            // when & then
            assertThatThrownBy(() -> createOrderUseCase.execute(input))
                    .isInstanceOf(CouponException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.COUPON_NOT_FOUND);

            verify(userCouponRepository).findByIdWithCoupon(userCouponId);
            verify(orderRepository, never()).save(any());
        }
    }
}
