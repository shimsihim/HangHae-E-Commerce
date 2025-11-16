//package io.hhplus.tdd.domain.order.application;
//
//import io.hhplus.tdd.domain.coupon.domain.model.Coupon;
//import io.hhplus.tdd.domain.coupon.domain.model.DiscountType;
//import io.hhplus.tdd.domain.coupon.domain.model.Status;
//import io.hhplus.tdd.domain.coupon.domain.model.UserCoupon;
//import io.hhplus.tdd.domain.coupon.infrastructure.repository.CouponRepository;
//import io.hhplus.tdd.domain.coupon.infrastructure.repository.UserCouponRepository;
//import io.hhplus.tdd.domain.coupon.domain.service.CouponService;
//import io.hhplus.tdd.domain.order.domain.model.Order;
//import io.hhplus.tdd.domain.order.infrastructure.repository.OrderItemRepository;
//import io.hhplus.tdd.domain.order.infrastructure.repository.OrderRepository;
//import io.hhplus.tdd.domain.order.domain.service.OrderService;
//import io.hhplus.tdd.domain.point.domain.model.PointHistory;
//import io.hhplus.tdd.domain.point.domain.model.UserPoint;
//import io.hhplus.tdd.domain.point.infrastructure.repository.PointHistoryRepository;
//import io.hhplus.tdd.domain.point.infrastructure.repository.UserPointRepository;
//import io.hhplus.tdd.domain.point.domain.service.PointService;
//import io.hhplus.tdd.domain.product.domain.model.Product;
//import io.hhplus.tdd.domain.product.domain.model.ProductOption;
//import io.hhplus.tdd.domain.product.infrastructure.repository.ProductOptionRepository;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Nested;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.Spy;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.mockito.junit.jupiter.MockitoSettings;
//import org.mockito.quality.Strictness;
//
//import java.time.LocalDate;
//import java.util.List;
//import java.util.Optional;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.BDDMockito.given;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//@MockitoSettings(strictness = Strictness.LENIENT)
//class MakeOrderUseCaseTest {
//
//    MakeOrderUseCase makeOrderUseCase;
//
//    @Mock
//    OrderRepository orderRepository;
//
//    @Mock
//    OrderItemRepository orderItemRepository;
//
//    @Mock
//    ProductOptionRepository productOptionRepository;
//
//    @Mock
//    CouponRepository couponRepository;
//
//    @Mock
//    UserCouponRepository userCouponRepository;
//
//    @Mock
//    UserPointRepository userPointRepository;
//
//    @Mock
//    PointHistoryRepository pointHistoryRepository;
//
//    CouponService couponService;
//    PointService pointService;
//    OrderService orderService;
//
//    @BeforeEach
//    void setUp() {
//        couponService = new CouponService();
//        pointService = new PointService();
//        orderService = new OrderService();
//
//        makeOrderUseCase = new MakeOrderUseCase(
//                orderRepository,
//                orderItemRepository,
//                productOptionRepository,
//                couponRepository,
//                userCouponRepository,
//                userPointRepository,
//                pointHistoryRepository,
//                couponService,
//                pointService,
//                orderService
//        );
//    }
//
//    @Nested
//    class 주문_성공 {
//
//        @Test
//        void 쿠폰_사용_주문() {
//            // given
//            long userId = 1L;
//            long productId = 1L;
//            long productOptionId = 1L;
//            long userCouponId = 1L;
//            long quantity = 2L;
//            long price = 10000L;
//            long totalAmount = price * quantity;
//            long discountAmount = 2000L;
//
//            Product product = Product.builder()
//                    .id(productId)
//                    .name("테스트 상품")
//                    .basePrice(price)
//                    .build();
//
//            ProductOption productOption = ProductOption.builder()
//                    .id(productOptionId)
//                    .product(product)
//                    .productId(productId)
//                    .optionName("기본옵션")
//                    .price(price)
//                    .quantity(10L)
//                    .build();
//
//            UserPoint userPoint = UserPoint.builder()
//                    .id(userId)
//                    .balance(50000L)
//                    .version(0L)
//                    .build();
//
//            Coupon coupon = Coupon.builder()
//                    .id(1L)
//                    .discountType(DiscountType.FIXED_AMOUNT)
//                    .discountValue(2000)
//                    .minOrderValue(5000)
//                    .validFrom(LocalDate.now().minusDays(1))
//                    .validUntil(LocalDate.now().plusDays(30))
//                    .build();
//
//            UserCoupon userCoupon = UserCoupon.builder()
//                    .id(userCouponId)
//                    .userId(userId)
//                    .couponId(1L)
//                    .status(Status.ISSUED)
//                    .expiredAt(LocalDate.now().plusDays(30))
//                    .build();
//
//            given(productOptionRepository.findById(1L)).willReturn(Optional.of(productOption));
//            given(userCouponRepository.findByUserIdAndIdAndStatus(1L, 1L, Status.ISSUED))
//                    .willReturn(Optional.of(userCoupon));
//            given(couponRepository.findById(1L)).willReturn(Optional.of(coupon));
//            given(userPointRepository.findById(1L)).willReturn(Optional.of(userPoint));
//            given(orderRepository.save(any(Order.class))).willAnswer(invocation -> {
//                Order arg = invocation.getArgument(0);
//                return Order.builder()
//                        .id(1L)
//                        .userId(arg.getUserId())
//                        .userCouponId(arg.getUserCouponId())
//                        .totalAmount(arg.getTotalAmount())
//                        .discountAmount(arg.getDiscountAmount())
//                        .usePointAmount(arg.getUsePointAmount())
//                        .finalAmount(arg.getFinalAmount())
//                        .status(arg.getStatus())
//                        .build();
//            });
//            given(productOptionRepository.save(any(ProductOption.class))).willAnswer(invocation -> invocation.getArgument(0));
//            given(userCouponRepository.save(any(UserCoupon.class))).willAnswer(invocation -> invocation.getArgument(0));
//            given(orderItemRepository.save(any())).willReturn(null);
//
//            // when
//            MakeOrderUseCase.Input input = MakeOrderUseCase.Input.of(
//                    userId,
//                    List.of(MakeOrderUseCase.Input.ItemInfo.of(1L, productOptionId, quantity)),
//                    userCouponId,
//                    null
//            );
//            MakeOrderUseCase.Output output = makeOrderUseCase.execute(input);
//
//            // then
//            assertThat(output.totalAmount()).isEqualTo(totalAmount);
//            assertThat(output.discountAmount()).isEqualTo(discountAmount);
//            assertThat(userCoupon.getStatus()).isEqualTo(Status.USED);
//            assertThat(productOption.getQuantity()).isEqualTo(10L - quantity);
//            verify(userCouponRepository, atLeastOnce()).save(userCoupon);
//        }
//
//        @Test
//        void 포인트_사용_주문() {
//            // given
//            long userId = 1L;
//            long productId = 1L;
//            long productOptionId = 1L;
//            long quantity = 2L;
//            long price = 10000L;
//            long totalAmount = price * quantity;
//            long usePointAmount = 5000L;
//
//            Product product = Product.builder()
//                    .id(productId)
//                    .name("테스트 상품")
//                    .basePrice(price)
//                    .build();
//
//            ProductOption productOption = ProductOption.builder()
//                    .id(productOptionId)
//                    .product(product)
//                    .productId(productId)
//                    .optionName("기본옵션")
//                    .price(price)
//                    .quantity(10L)
//                    .build();
//
//            UserPoint userPoint = UserPoint.builder()
//                    .id(userId)
//                    .balance(50000L)
//                    .version(0L)
//                    .build();
//
//            given(productOptionRepository.findById(anyLong())).willReturn(Optional.of(productOption));
//            given(userPointRepository.findById(anyLong())).willReturn(Optional.of(userPoint));
//            given(orderRepository.save(any(Order.class))).willAnswer(invocation -> {
//                Order arg = invocation.getArgument(0);
//                return Order.builder()
//                        .id(1L)
//                        .userId(arg.getUserId())
//                        .userCouponId(arg.getUserCouponId())
//                        .totalAmount(arg.getTotalAmount())
//                        .discountAmount(arg.getDiscountAmount())
//                        .usePointAmount(arg.getUsePointAmount())
//                        .finalAmount(arg.getFinalAmount())
//                        .status(arg.getStatus())
//                        .build();
//            });
//            given(productOptionRepository.save(any(ProductOption.class))).willAnswer(invocation -> invocation.getArgument(0));
//            given(userPointRepository.save(any(UserPoint.class))).willAnswer(invocation -> invocation.getArgument(0));
//            given(pointHistoryRepository.save(any(PointHistory.class))).willAnswer(invocation -> invocation.getArgument(0));
//            given(orderItemRepository.save(any())).willReturn(null);
//
//            // when
//            MakeOrderUseCase.Input input = MakeOrderUseCase.Input.of(
//                    userId,
//                    List.of(MakeOrderUseCase.Input.ItemInfo.of(1L, productOptionId, quantity)),
//                    null,
//                    usePointAmount
//            );
//            MakeOrderUseCase.Output output = makeOrderUseCase.execute(input);
//
//            // then
//            assertThat(output.totalAmount()).isEqualTo(totalAmount);
//            assertThat(output.usePointAmount()).isEqualTo(usePointAmount);
//            assertThat(output.finalAmount()).isEqualTo(totalAmount - usePointAmount);
//            assertThat(userPoint.getBalance()).isEqualTo(50000L - usePointAmount);
//            assertThat(productOption.getQuantity()).isEqualTo(10L - quantity);
//            verify(pointHistoryRepository, atLeastOnce()).save(any(PointHistory.class));
//        }
//    }
//}
