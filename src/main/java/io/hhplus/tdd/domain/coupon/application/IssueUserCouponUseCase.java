package io.hhplus.tdd.domain.coupon.application;

import io.hhplus.tdd.common.exception.ErrorCode;
import io.hhplus.tdd.common.lock.LockAnn;
import io.hhplus.tdd.common.lock.LockId;
import io.hhplus.tdd.common.lock.LockKey;
import io.hhplus.tdd.domain.coupon.domain.model.Coupon;
import io.hhplus.tdd.domain.coupon.domain.model.UserCoupon;
import io.hhplus.tdd.domain.coupon.domain.repository.CouponRepository;
import io.hhplus.tdd.domain.coupon.domain.repository.UserCouponRepository;
import io.hhplus.tdd.domain.coupon.domain.service.CouponService;
import io.hhplus.tdd.domain.coupon.exception.CouponException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 쿠폰 발급 애플리케이션 서비스
 * - 쿠폰 발급 유스케이스의 흐름을 조율합니다.
 * - Repository를 통한 데이터 조회/저장을 담당합니다.
 * - Domain Service에 비즈니스 로직을 위임합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IssueUserCouponUseCase {

    private final UserCouponRepository userCouponRepository;
    private final CouponRepository couponRepository;
    private final CouponService couponService;

    public record Input(
            @LockId long couponId,
            long userId
    ){}

    @LockAnn(lockKey = LockKey.COUPON)
    public void execute(Input input){
        // 1. 쿠폰 조회
        Coupon coupon = couponRepository.findById(input.couponId())
                .orElseThrow(() -> new CouponException(ErrorCode.COUPON_NOT_FOUND, input.couponId()));

        // 발급 전 수량 백업 (롤백용)
        int originalIssuedQuantity = coupon.getIssuedQuantity();

        // 2. 사용자가 발급받은 쿠폰 목록 조회
        List<UserCoupon> userIssuedCoupons = userCouponRepository.findByUserIdAndCouponId(input.userId(), input.couponId());

        try {
            // 3. 도메인 서비스를 통한 쿠폰 발급 로직 실행
            UserCoupon userCoupon = couponService.issueCoupon(coupon, input.userId(), userIssuedCoupons);

            // 4. 변경된 쿠폰 정보 저장
            couponRepository.save(coupon);

            // 5. 발급된 사용자 쿠폰 저장
            userCouponRepository.save(userCoupon);

        } catch (Exception e) {
            log.error("쿠폰 발급 실패. 롤백 수행. userId: {}, couponId: {}", input.userId(), input.couponId(), e);

            while (coupon.getIssuedQuantity() > originalIssuedQuantity) {
                coupon.decreaseIssuedQuantity();
            }
            couponRepository.save(coupon);
            throw e;
        }
    }

}
