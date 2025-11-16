//package io.hhplus.tdd.domain.order.application;
//
//import io.hhplus.tdd.common.exception.ErrorCode;
//import io.hhplus.tdd.common.exception.UserNotFoundException;
//import io.hhplus.tdd.domain.coupon.domain.model.Coupon;
//import io.hhplus.tdd.domain.coupon.domain.model.Status;
//import io.hhplus.tdd.domain.coupon.domain.model.UserCoupon;
//import io.hhplus.tdd.domain.coupon.infrastructure.repository.CouponRepository;
//import io.hhplus.tdd.domain.coupon.infrastructure.repository.UserCouponRepository;
//import io.hhplus.tdd.domain.coupon.domain.service.CouponService;
//import io.hhplus.tdd.domain.coupon.exception.CouponException;
//import io.hhplus.tdd.domain.order.domain.model.Order;
//import io.hhplus.tdd.domain.order.domain.model.OrderItem;
//import io.hhplus.tdd.domain.order.infrastructure.repository.OrderItemRepository;
//import io.hhplus.tdd.domain.order.infrastructure.repository.OrderRepository;
//import io.hhplus.tdd.domain.order.domain.service.OrderService;
//import io.hhplus.tdd.domain.point.domain.model.PointHistory;
//import io.hhplus.tdd.domain.point.domain.model.UserPoint;
//import io.hhplus.tdd.domain.point.infrastructure.repository.PointHistoryRepository;
//import io.hhplus.tdd.domain.point.infrastructure.repository.UserPointRepository;
//import io.hhplus.tdd.domain.point.domain.service.PointService;
//import io.hhplus.tdd.domain.point.exception.PointRangeException;
//import io.hhplus.tdd.domain.product.domain.model.ProductOption;
//import io.hhplus.tdd.domain.product.infrastructure.repository.ProductOptionRepository;
//import io.hhplus.tdd.domain.product.exception.ProductException;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class MakeOrderUseCase {
//
//    private final OrderRepository orderRepository;
//    private final OrderItemRepository orderItemRepository;
//    private final ProductOptionRepository productOptionRepository;
//    private final CouponRepository couponRepository;
//    private final UserCouponRepository userCouponRepository;
//    private final UserPointRepository userPointRepository;
//    private final PointHistoryRepository pointHistoryRepository;
//
//    private final CouponService couponService;
//    private final PointService pointService;
//    private final OrderService orderService;
//
//    public record Input(
//            Long userId,
//            List<ItemInfo> items,
//            Long userCouponId,
//            Long usePointAmount
//    ) {
//        public static Input of(Long userId, List<ItemInfo> items,
//                               Long userCouponId,
//                               Long usePointAmount){
//            return new Input(userId, items, userCouponId, usePointAmount);
//        }
//
//        public record ItemInfo(
//                Long productId,
//                Long productOptionId,
//                Long quantity
//        ){
//            public static ItemInfo of(long productId, long productOptionId, long quantity){
//                return new ItemInfo(productId, productOptionId, quantity);
//            }
//        }
//    }
//
//    public record Output(
//            Long orderId,
//            Long userId,
//            Long totalAmount,
//            Long discountAmount,
//            Long usePointAmount,
//            Long finalAmount
//    ) {
//        public static Output from(Order order) {
//            return new Output(
//                    order.getId(),
//                    order.getUserId(),
//                    order.getTotalAmount(),
//                    order.getDiscountAmount(),
//                    order.getUsePointAmount(),
//                    order.getFinalAmount()
//            );
//        }
//    }
//
//
//    @Transactional
//    public Output execute(Input input) {
//
//        Map<Long, ProductOption> productOptionMap = new HashMap<>();
//
//        // 상품 옵션 조회 및 원본 재고 보관
//        for (Input.ItemInfo item : input.items()) {
//            ProductOption productOption = productOptionRepository.findById(item.productOptionId())
//                    .orElseThrow(() -> new ProductException(ErrorCode.PRODUCT_NOT_FOUND, item.productId(), item.productOptionId()));
//
//            productOptionMap.put(item.productOptionId(), productOption);
//        }
//
//        //  총 주문 금액 계산
//        List<OrderService.OrderItemInfo> orderItems = input.items().stream()
//                .map(item -> OrderService.OrderItemInfo.of(item.productId(), item.productOptionId() ,productOptionMap.get(item.productOptionId()).getPrice() , item.quantity()))
//                .toList();
//
//        long totalAmount = orderService.calculateTotalAmount(orderItems);
//
//        // 쿠폰 검증 및 할인 금액 계산
//        long discountAmount = 0;
//        Coupon coupon = null;
//        UserCoupon userCoupon = null;
//
//        if (input.userCouponId() != null) {
//            userCoupon = userCouponRepository.findByUserIdAndIdAndStatus(
//                            input.userId(), input.userCouponId(), Status.ISSUED)
//                    .orElseThrow(() -> new CouponException(ErrorCode.COUPON_NOT_FOUND, input.userCouponId()));
//
//            long couponId = userCoupon.getCouponId();
//            coupon = couponRepository.findById(userCoupon.getCouponId())
//                    .orElseThrow(() -> new CouponException(ErrorCode.COUPON_NOT_FOUND, couponId));
//
//            discountAmount = couponService.useUserCoupon(coupon, userCoupon, totalAmount);
//        }
//
//        // 포인트 검증
//        long usePointAmount = input.usePointAmount() != null ? input.usePointAmount() : 0;
//
//        UserPoint userPoint = userPointRepository.findById(input.userId())
//                .orElseThrow(() -> new UserNotFoundException(ErrorCode.USER_NOT_FOUND, input.userId()));
//        long originalBalance = userPoint.getBalance();
//
//        if (userPoint.getBalance() < usePointAmount) {
//            throw new PointRangeException(ErrorCode.USER_POINT_NOT_ENOUGH, input.userId(), userPoint.getBalance() , usePointAmount);
//        }
//
//        // 주문 생성 (PENDING 상태)
//        Order order = Order.createOrder(
//                input.userId(),
//                input.userCouponId(),
//                totalAmount,
//                discountAmount,
//                usePointAmount
//        );
//        Order savedOrder = orderRepository.save(order);
//
//        // 재고 차감 (도메인 서비스 사용)
//        orderService.validateAndDeductStock(orderItems, productOptionMap);
//        for (Input.ItemInfo item : input.items()) {
//            ProductOption productOption = productOptionMap.get(item.productOptionId());
//            productOptionRepository.save(productOption);
//        }
//
//        // 쿠폰 사용 처리 (이미 applyDiscount에서 처리됨)
//        if (userCoupon != null) {
//            userCouponRepository.save(userCoupon);
//        }
//
//        // 포인트 차감
//        if (usePointAmount > 0) {
//            PointHistory pointHistory = pointService.usePoint(userPoint, usePointAmount, "주문 결제");
//            userPointRepository.save(userPoint);
//            pointHistoryRepository.save(pointHistory);
//        }
//
//        // 주문 완료 처리 (PAID 상태로 변경)
//        savedOrder.completeOrder();
//        savedOrder = orderRepository.save(savedOrder);
//
//        // 주문 항목 저장
//        for (Input.ItemInfo item : input.items()) {
//            ProductOption productOption = productOptionMap.get(item.productOptionId());
//            OrderItem orderItem = Order.createOrderItem(
//                    savedOrder,
//                    productOption.getProductId(),
//                    item.productOptionId(),
//                    item.quantity().intValue(),
//                    productOption.getPrice()
//            );
//            orderItemRepository.save(orderItem);
//        }
//
//        return Output.from(savedOrder);
//    }
//}
