package io.hhplus.tdd.domain.order.domain.service;

import io.hhplus.tdd.common.exception.ErrorCode;
import io.hhplus.tdd.domain.coupon.domain.model.Coupon;
import io.hhplus.tdd.domain.coupon.domain.model.UserCoupon;
import io.hhplus.tdd.domain.coupon.domain.service.CouponService;
import io.hhplus.tdd.domain.order.domain.model.Order;
import io.hhplus.tdd.domain.point.domain.model.PointHistory;
import io.hhplus.tdd.domain.point.domain.model.UserPoint;
import io.hhplus.tdd.domain.point.domain.service.PointService;
import io.hhplus.tdd.domain.point.infrastructure.repository.PointHistoryRepository;
import io.hhplus.tdd.domain.product.domain.model.Product;
import io.hhplus.tdd.domain.product.domain.model.ProductOption;
import io.hhplus.tdd.domain.product.exception.ProductException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 주문 도메인 서비스
 * - 주문 생성 관련 비즈니스 로직 담당
 * - UseCase는 흐름 조정만, 핵심 로직은 여기에 위치
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private final PointService pointService;
    private final CouponService couponService;
    private final PointHistoryRepository pointHistoryRepository;

    /**
     * 주문 가능 여부를 검증합니다 (재고, 상품 존재 여부)
     * @param products 주문할 상품 목록
     * @param requestedOptionMap 요청된 옵션 ID와 수량 맵
     */
    public void validateProductsExist(List<Product> products, Map<Long, RequestedOption> requestedOptionMap) {
        Set<Long> requestedOptionIds = requestedOptionMap.keySet();

        Set<Long> foundOptionIds = products.stream()
                .flatMap(product -> product.getOptions().stream())
                .map(ProductOption::getId)
                .collect(Collectors.toSet());

        Set<Long> missingOptionIds = requestedOptionIds.stream()
                .filter(id -> !foundOptionIds.contains(id))
                .collect(Collectors.toSet());

        if (!missingOptionIds.isEmpty()) {
            throw new ProductException(ErrorCode.PRODUCT_NOT_FOUNDS, missingOptionIds.toString());
        }
    }

    /**
     * 재고를 선점합니다 (차감)
     * @param products 재고 차감할 상품 목록
     * @param requestedOptionMap 요청된 옵션 ID와 수량 맵
     * @return 총 주문 금액 (할인 미적용)
     */
    public long reserveStock(List<Product> products, Map<Long, RequestedOption> requestedOptionMap) {
        long totalAmount = 0;

        for (Product product : products) {
            for (ProductOption option : product.getOptions()) {
                RequestedOption requested = requestedOptionMap.get(option.getId());
                if (requested != null) {
                    option.deduct(requested.quantity());
                    totalAmount += option.getPrice() * requested.quantity();
                }
            }
        }

        return totalAmount;
    }

    /**
     * 재고를 복구합니다 (주문 취소, 결제 실패 시)
     * @param products 재고 복구할 상품 목록
     * @param requestedOptionMap 복구할 옵션 ID와 수량 맵
     */
    public void releaseStock(List<Product> products, Map<Long, RequestedOption> requestedOptionMap) {
        for (Product product : products) {
            for (ProductOption option : product.getOptions()) {
                RequestedOption requested = requestedOptionMap.get(option.getId());
                if (requested != null) {
                    option.restore(requested.quantity());
                }
            }
        }
    }

    /**
     * 즉시 완료 주문 처리 (finalAmount가 0원인 경우)
     * - 포인트 차감
     * - 쿠폰 사용
     * - 주문 상태 PAID 변경
     * @param order 주문 엔티티
     */
    public void completeImmediateOrder(Order order) {
        UserPoint userPoint = order.getUserPoint();
        UserCoupon userCoupon = order.getUserCoupon();
        long usePointAmount = order.getUsePointAmount();
        long totalAmount = order.getTotalAmount();

        // 포인트 차감
        if (usePointAmount > 0) {
            PointHistory pointHistory = pointService.usePoint(userPoint, usePointAmount, "Buy Product");
            pointHistoryRepository.save(pointHistory);
        }

        // 쿠폰 사용
        if (userCoupon != null) {
            Coupon coupon = userCoupon.getCoupon();
            couponService.useUserCoupon(coupon, userCoupon, totalAmount);
        }

        // 주문 완료 처리
        order.completeOrder();
    }

    /**
     * PG 결제 완료 후 주문 완료 처리
     * - PayCompleteOrderUseCase에서 호출
     * - 포인트 차감, 쿠폰 사용, 주문 상태 변경
     * @param order 주문 엔티티
     */
    public void completeOrderWithPayment(Order order) {
        UserPoint userPoint = order.getUserPoint();
        UserCoupon userCoupon = order.getUserCoupon();
        long usePointAmount = order.getUsePointAmount();
        long totalAmount = order.getTotalAmount();

        // 포인트 차감
        if (usePointAmount > 0) {
            PointHistory pointHistory = pointService.usePoint(userPoint, usePointAmount, "Buy Product");
            pointHistoryRepository.save(pointHistory);
        }

        // 쿠폰 사용
        if (userCoupon != null) {
            Coupon coupon = userCoupon.getCoupon();
            couponService.useUserCoupon(coupon, userCoupon, totalAmount);
        }

        // 주문 완료 처리
        order.completeOrder();
    }

    /**
     * 주문 취소 처리
     * - 재고 복구는 CancelOrderUseCase에서 처리
     * - 여기서는 주문 상태만 변경
     * @param order 취소할 주문
     */
    public void cancelOrder(Order order) {
        order.cancel();
    }

    /**
     * 요청된 옵션 정보 (DTO)
     */
    public record RequestedOption(
            Long productOptionId,
            int quantity
    ) {}
}
