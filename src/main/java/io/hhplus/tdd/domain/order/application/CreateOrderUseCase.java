package io.hhplus.tdd.domain.order.application;

import io.hhplus.tdd.common.exception.ErrorCode;
import io.hhplus.tdd.common.exception.UserNotFoundException;
import io.hhplus.tdd.domain.coupon.domain.model.UserCoupon;
import io.hhplus.tdd.domain.coupon.domain.service.CouponService;
import io.hhplus.tdd.domain.coupon.exception.CouponException;
import io.hhplus.tdd.domain.coupon.infrastructure.repository.UserCouponRepository;
import io.hhplus.tdd.domain.order.domain.model.Order;
import io.hhplus.tdd.domain.order.domain.model.OrderItem;
import io.hhplus.tdd.domain.order.domain.service.OrderService;
import io.hhplus.tdd.domain.order.infrastructure.repository.OrderItemRepository;
import io.hhplus.tdd.domain.order.infrastructure.repository.OrderRepository;
import io.hhplus.tdd.domain.point.domain.model.UserPoint;
import io.hhplus.tdd.domain.point.infrastructure.repository.UserPointRepository;
import io.hhplus.tdd.domain.product.domain.model.ProductOption;
import io.hhplus.tdd.domain.product.infrastructure.repository.ProductOptionRepository;
import io.hhplus.tdd.domain.product.infrastructure.repository.ProductRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreateOrderUseCase {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserPointRepository userPointRepository;
    private final ProductRepository productRepository; // 0원 결제 시 락 획득용
    private final UserCouponRepository userCouponRepository;
    private final ProductOptionRepository productOptionRepository;
    private final EntityManager entityManager;

    private final CouponService couponService;
    private final OrderService orderService;


    public record Input(
            Long userId,
            List<ProductInfo> items,
            Long userCouponId,
            long usePointAmount
    ) {
        public record ProductInfo(
                Long productOptionId,
                int quantity
        ) {}
    }

    public record Output(
            Long orderId,
            Long userId,
            Long totalAmount,
            Long discountAmount,
            Long usePointAmount,
            Long finalAmount
    ) {
        public static Output from(Order order) {
            return new Output(
                    order.getId(),
                    order.getUserId(),
                    order.getTotalAmount(),
                    order.getDiscountAmount(),
                    order.getUsePointAmount(),
                    order.getFinalAmount()
            );
        }
    }

    @Transactional
    public Output execute(Input input) {
        // 1. 사용자 포인트 조회 및 검증 (사용 가능 여부만 확인)
        UserPoint userPoint = userPointRepository.findById(input.userId())
                .orElseThrow(() -> new UserNotFoundException(ErrorCode.USER_NOT_FOUND, input.userId()));
        userPoint.validUsePoint(input.usePointAmount());

        // 2. 상품 옵션 조회 (Fetch Join으로 Product까지 함께 로딩)
        // Input -> Internal DTO 변환
        List<OrderService.OrderItemInfo> orderItemsInfo = input.items().stream()
                .map(item -> new OrderService.OrderItemInfo(item.productOptionId(), item.quantity()))
                .toList();

        List<Long> optionIds = orderItemsInfo.stream()
                .map(OrderService.OrderItemInfo::productOptionId)
                .toList();

        // 여기서 이미 Product 정보가 포함된 ProductOption을 가져옵니다.
        List<ProductOption> productOptions = productOptionRepository.findAllWithProductByIdIn(optionIds);

        // 재고 검증 및 총 주문 금액 계산 (도메인 서비스 위임)
        long totalAmount = orderService.validOrder(productOptions, orderItemsInfo);

        // 3. 쿠폰 조회 및 검증
        UserCoupon userCoupon = null;
        long discountAmount = 0;
        if (input.userCouponId() != null) {
            userCoupon = userCouponRepository.findByIdWithCoupon(input.userCouponId())
                    .orElseThrow(() -> new CouponException(ErrorCode.COUPON_NOT_FOUND, input.userCouponId()));

            // 쿠폰 사용 가능 여부 및 할인 금액 계산
            discountAmount = couponService.validateCouponUsage(userCoupon.getCoupon(), userCoupon, totalAmount);
        }

        // 4. 주문 객체 생성 (아직 저장은 안 함)
        Order newOrder = Order.createOrder(userPoint, userCoupon, totalAmount, discountAmount, input.usePointAmount());

        // 5. 0원 결제 처리 (즉시 완료)
        if (newOrder.getFinalAmount() == 0) {
            // JPA 1차 캐시 문제 해결: 이전에 조회한 ProductOption을 영속성 컨텍스트에서 제거
            productOptions.forEach(entityManager::detach);

            // 재고 차감을 위해 비관적 락으로 Product(또는 Option)를 다시 조회해야 함
            // (참고: 단순히 createOrderItems를 위해 productOptions를 재사용하는 것과 별개로,
            // 동시성 제어를 위해 락이 걸린 엔티티가 필요함)
            List<ProductOption> productsOptionsWithLock = productOptionRepository.findAllByIdInForUpdate(optionIds);

            // 재고 차감, 포인트 차감, 쿠폰 사용 처리 (락이 걸린 엔티티 사용)
            orderService.completeOrderWithPayment(newOrder, productsOptionsWithLock, orderItemsInfo);

            // OrderItem 생성 시 사용할 수 있도록 락이 걸린 엔티티로 교체
            productOptions = productsOptionsWithLock;
        }

        // 6. 주문 저장
        Order savedOrder = orderRepository.save(newOrder);

        // 7. 주문 항목(OrderItem) 생성 및 저장
        List<OrderItem> orderItemEntities = createOrderItems(savedOrder, productOptions, orderItemsInfo);
        orderItemRepository.saveAll(orderItemEntities);

        return Output.from(savedOrder);
    }

    /**
     * OrderItem 목록 생성 메서드
     * - productOptions에는 이미 product가 fetch join 되어 있다고 가정
     */
    private List<OrderItem> createOrderItems(Order savedOrder,
                                             List<ProductOption> productOptions,
                                             List<OrderService.OrderItemInfo> orderItemsInfo) {

        // List -> Map 변환 (조회 성능 O(1))
        Map<Long, ProductOption> optionMap = productOptions.stream()
                .collect(Collectors.toMap(ProductOption::getId, Function.identity()));

        return orderItemsInfo.stream()
                .map(info -> {
                    // Map에서 옵션 꺼내기
                    ProductOption option = optionMap.get(info.productOptionId());

                    if (option == null) {
                        throw new IllegalArgumentException("상품 옵션 정보가 유효하지 않습니다. ID: " + info.productOptionId());
                    }

                    // OrderItem 생성 (ProductOption 안에 있는 Product를 꺼내서 사용)
                    return OrderItem.create(
                            savedOrder,
                            option.getProduct(), // Fetch Join 되었으므로 즉시 접근 가능
                            option,
                            info.quantity(),
                            option.getPrice()
                    );
                })
                .collect(Collectors.toList());
    }
}