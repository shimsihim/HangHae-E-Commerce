package io.hhplus.tdd.domain.order.domain.service;

import io.hhplus.tdd.common.cache.CacheEvictionService;
import io.hhplus.tdd.common.exception.ErrorCode;
import io.hhplus.tdd.domain.coupon.domain.model.Coupon;
import io.hhplus.tdd.domain.coupon.domain.model.UserCoupon;
import io.hhplus.tdd.domain.coupon.domain.service.CouponService;
import io.hhplus.tdd.domain.order.application.OrderEventPublisher;
import io.hhplus.tdd.domain.order.domain.model.Order;
import io.hhplus.tdd.domain.order.exception.OrderException;
import io.hhplus.tdd.domain.point.domain.model.PointHistory;
import io.hhplus.tdd.domain.point.domain.model.UserPoint;
import io.hhplus.tdd.domain.point.domain.service.PointService;
import io.hhplus.tdd.domain.point.infrastructure.repository.PointHistoryRepository;
import io.hhplus.tdd.domain.product.domain.model.Product;
import io.hhplus.tdd.domain.product.domain.model.ProductOption;
import io.hhplus.tdd.domain.product.domain.service.RankingService;
import io.hhplus.tdd.domain.product.exception.ProductException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 주문 도메인 서비스
 * - 주문 생성, 검증, 결제 완료 처리 관련 비즈니스 로직
 * - UseCase는 흐름 조정만, 핵심 로직은 여기에 위치
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private final PointService pointService;
    private final CouponService couponService;
    private final PointHistoryRepository pointHistoryRepository;
    private final CacheEvictionService cacheEvictionService;
    private final RankingService rankingService;
    private final OrderEventPublisher orderEventPublisher;

    /**
     * 주문 항목 정보
     */
    public record OrderItemInfo(
            Long productOptionId,
            int quantity
    ) {}

    /**
     * 주문 가능 여부를 검증합니다
     * - 상품/옵션 존재 여부 검증
     * - 재고 충분 여부 검증 (차감하지 않음)
     *
     * @param productOptions 조회된 상품옵션 목록
     * @param orderItems 주문 항목 목록
     * @return 검증 결과 (총 주문 금액)
     */
    public long validOrder(List<ProductOption> productOptions , List<OrderItemInfo> orderItems){
        if(productOptions.size() == 0 || productOptions.size() != orderItems.size()){
            throw new OrderException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        long totalAmount = 0;

        for(OrderItemInfo info : orderItems){
            ProductOption po = productOptions.stream()
                    .filter(option -> option.getId().equals(info.productOptionId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("해당하는 상품 옵션을 찾을 수 없습니다."));
            totalAmount += po.validateStock(info.quantity());
        }
        return totalAmount;
    }


    /**
     * 재고를 차감합니다 (결제 완료 시점에 호출)
     * 재고 부족 임계값(10개) 미만으로 떨어지면 캐시 무효화
     * 판매량 랭킹 업데이트
     *
     * @param productOptions 재고 차감할 상품옵션 목록
     * @param orderItems 주문 항목 목록
     */
    public void deductStock(List<ProductOption> productOptions, List<OrderItemInfo> orderItems) {
        Map<Long, ProductOption> optionMap = productOptions.stream()
                .collect(Collectors.toMap(ProductOption::getId, Function.identity()));

        for (OrderItemInfo item : orderItems) {
            ProductOption option = optionMap.get(item.productOptionId());
            if (option != null) {
                option.deduct(item.quantity());

                // 재고 차감 후 재고가 부족하면 캐시 무효화
                Long currentStock = option.getQuantity();
                Long productId = option.getProductId();
                cacheEvictionService.evictIfLowStock(productId, currentStock);

                // 판매량 랭킹 업데이트 (판매 수량만큼 점수 증가)
                rankingService.addScore(productId, item.quantity());
            }
        }
    }

    /**
     * 재고를 복구합니다 (주문 취소, 결제 실패 시)
     *
     * @param productOptions 재고 복구할 상품옵션 목록
     * @param orderItems 주문 항목 목록
     */
    public void restoreStock(List<ProductOption> productOptions, List<OrderItemInfo> orderItems) {
        Map<Long, ProductOption> optionMap = productOptions.stream()
                .collect(Collectors.toMap(ProductOption::getId, Function.identity()));

        for (OrderItemInfo item : orderItems) {
            ProductOption option = optionMap.get(item.productOptionId());
            if (option != null) {
                option.restore(item.quantity());
            }
        }
    }

    /**
     * PG 결제 완료 후 주문 완료 처리
     * - 재고 차감
     * - 포인트 차감
     * - 쿠폰 사용
     * - 주문 상태 PAID 변경
     * - 주문 완료 이벤트 발행
     *
     * @param order 주문 엔티티
     * @param productOptions 상품 목록
     * @param orderItems 주문 항목 목록
     */
    public void completeOrderWithPayment(Order order, List<ProductOption> productOptions, List<OrderItemInfo> orderItems) {
        // 1. 재고 차감
        deductStock(productOptions, orderItems);

        // 2. 포인트 차감
        processPointDeduction(order);

        // 3. 쿠폰 사용
        processCouponUsage(order);

        // 4. 주문 완료 처리
        order.completeOrder();

        // 5. 주문 완료 이벤트 발행
        orderEventPublisher.publishOrderCompletedEvent(order);
    }


    /**
     * 포인트 차감 처리 (private 헬퍼 메서드)
     */
    private void processPointDeduction(Order order) {
        UserPoint userPoint = order.getUserPoint();
        long usePointAmount = order.getUsePointAmount();

        if (usePointAmount > 0) {
            PointHistory pointHistory = pointService.usePoint(userPoint, usePointAmount, "Buy Product");
            pointHistoryRepository.save(pointHistory);
        }
    }

    /**
     * 쿠폰 사용 처리 (private 헬퍼 메서드)
     */
    private void processCouponUsage(Order order) {
        UserCoupon userCoupon = order.getUserCoupon();

        if (userCoupon != null) {
            Coupon coupon = userCoupon.getCoupon();
            long totalAmount = order.getTotalAmount();
            couponService.useUserCoupon(coupon, userCoupon, totalAmount);
        }
    }

    /**
     * 주문 취소 처리
     * - 재고 복구는 CancelOrderUseCase에서 처리
     * - 여기서는 주문 상태만 변경
     *
     * @param order 취소할 주문
     */
    public void cancelOrder(Order order) {
        order.cancel();
    }
}