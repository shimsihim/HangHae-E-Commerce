package io.hhplus.tdd.domain.coupon.application;

import io.hhplus.tdd.common.exception.ErrorCode;
import io.hhplus.tdd.domain.coupon.domain.model.Status;
import io.hhplus.tdd.domain.coupon.domain.model.UserCoupon;
import io.hhplus.tdd.domain.coupon.domain.repository.UserCouponRepository;
import io.hhplus.tdd.domain.coupon.domain.service.CouponService;
import io.hhplus.tdd.domain.coupon.exception.CouponException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


@Slf4j
@Service
@RequiredArgsConstructor
public class UseUserCouponUseCase {
    private final UserCouponRepository userCouponRepository;
    private final CouponService couponService;

    public record Input(
        long userId,
        long userCouponId
    ){}

    public void execute(Input input){
        // 1. 사용자 쿠폰 조회 (ISSUED 상태만)
        UserCoupon userCoupon = userCouponRepository.findByUserIdAndUserCouponIdAndStatus(
                input.userId(), input.userCouponId(), Status.ISSUED.toString())
                .orElseThrow(() -> new CouponException(ErrorCode.COUPON_NOT_FOUND, input.userCouponId));

        // 사용 전 상태 백업 (롤백용)
        Status originalStatus = userCoupon.getStatus();

        try {
            // 2. 도메인 서비스를 통한 쿠폰 사용 로직 실행
            couponService.useCoupon(userCoupon);

            // 3. 변경된 사용자 쿠폰 저장
            userCouponRepository.save(userCoupon);

        } catch (Exception e) {
            // 롤백: 쿠폰 상태 원복
            log.error("쿠폰 사용 실패. 롤백 수행. userId: {}, userCouponId: {}", input.userId(), input.userCouponId(), e);
            couponService.cancelUsage(userCoupon);
            userCouponRepository.save(userCoupon);
            throw e;
        }
    }
}
