package io.hhplus.tdd.domain.coupon.domain.service;

import io.hhplus.tdd.common.exception.ErrorCode;
import io.hhplus.tdd.domain.coupon.domain.model.Coupon;
import io.hhplus.tdd.domain.coupon.domain.model.UserCoupon;
import io.hhplus.tdd.domain.coupon.exception.CouponException;
import io.hhplus.tdd.domain.coupon.infrastructure.repository.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class CouponService {

    private final UserCouponRepository userCouponRepository;

    // 쿠폰 발급 가능 여부를 검증하고 발급 처리합니다.
    public UserCoupon issueCoupon(Coupon coupon, long userId) {

        List<UserCoupon> userIssuedCoupons = userCouponRepository.findByUserIdAndCouponId(userId, coupon. getId());
        // 1. 쿠폰 발급 검증 (유효기간, 발급 가능 수량)
        coupon.validIssue();

        // 2. 사용자별 발급 제한 검증
        validateUserIssueLimit(coupon, userIssuedCoupons);

        // 3. 쿠폰 발급 수량 증가 (도메인 로직)
        coupon.increaseIssuedQuantity();

        // 4. UserCoupon 생성
        return UserCoupon.from(userId, coupon);
    }

    /**
     * 쿠폰 사용 가능 여부 검증 (사전 검증용 - 주문 생성 시)
     * - 최소 주문 금액 충족 여부
     * - 사용자 쿠폰 상태 (만료, 이미 사용)
     * - 실제 사용은 하지 않음
     * @return 예상 할인 금액
     */
    public long validateCouponUsage(Coupon coupon, UserCoupon userCoupon, long orderAmount) {
        // 1. 최소 주문 금액 검증
        validateMinOrderValue(coupon, orderAmount);

        // 2. 사용자 쿠폰 검증만 (상태 변경 없음)
        userCoupon.validUseCoupon();

        // 3. 할인 금액 계산
        return coupon.calculateDiscountAmount(orderAmount);
    }

    /**
     * 쿠폰 실제 사용 처리 (결제 완료 시)
     * - 내부에서 검증 재수행 (TOCTOU 방지)
     * - 쿠폰 상태를 USED로 변경
     * @return 할인 금액
     */
    public long useUserCoupon(Coupon coupon, UserCoupon userCoupon, long orderAmount) {
        // 1. 최소 주문 금액 검증 (재검증)
        validateMinOrderValue(coupon, orderAmount);

        // 2. 쿠폰 사용 처리 (검증 + 상태 변경)
        userCoupon.useCoupon();

        // 3. 할인 금액 계산
        return coupon.calculateDiscountAmount(orderAmount);
    }

    // 최소 주문 금액을 검증
    private void validateMinOrderValue(Coupon coupon, long orderAmount) {
        if (orderAmount < coupon.getMinOrderValue()) {
            throw new CouponException(ErrorCode.COUPON_MIN_ORDER_VALUE_ERR, coupon.getId());
        }
    }

    // 사용자별 발급 제한을 검증합니다.
    private void validateUserIssueLimit(Coupon coupon, List<UserCoupon> userIssuedCoupons) {
        List<UserCoupon> sameUserCoupons = userIssuedCoupons.stream().filter(userCoupon -> userCoupon.getCouponId() == coupon.getId()).toList();
        if (sameUserCoupons.size() >= coupon.getLimitPerUser()) {
            throw new CouponException(ErrorCode.COUPON_ISSUE_LIMIT_PER_USER, coupon.getId());
        }
    }
}
