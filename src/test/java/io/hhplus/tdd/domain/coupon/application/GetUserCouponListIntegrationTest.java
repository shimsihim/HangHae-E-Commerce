package io.hhplus.tdd.domain.coupon.application;

import io.hhplus.tdd.domain.IntegrationTest;
import io.hhplus.tdd.domain.coupon.domain.model.Coupon;
import io.hhplus.tdd.domain.coupon.domain.model.DiscountType;
import io.hhplus.tdd.domain.coupon.domain.model.Status;
import io.hhplus.tdd.domain.coupon.domain.model.UserCoupon;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GetUserCouponListIntegrationTest extends IntegrationTest {

    @Autowired
    private GetUserCouponListUseCase getUserCouponListUseCase;

    @Test
    @DisplayName("사용자 쿠폰 목록 조회 통합 테스트 - 사용자가 발급받은 쿠폰을 정상적으로 조회한다")
    void 사용자_쿠폰_목록_조회_성공() {
        // given
        long userId = 1L;

        Coupon coupon1 = Coupon.builder()
                .couponName("신규 회원 할인 쿠폰")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(10)
                .totalQuantity(100)
                .issuedQuantity(50)
                .limitPerUser(1)
                .duration(30)
                .minOrderValue(10000)
                .validFrom(LocalDate.now().minusDays(1))
                .validUntil(LocalDate.now().plusDays(30))
                .build();

        Coupon savedCoupon1 = couponRepository.save(coupon1);

        UserCoupon userCoupon1 = UserCoupon.builder()
                .userId(userId)
                .couponId(savedCoupon1.getId())
                .coupon(savedCoupon1)
                .status(Status.ISSUED)
                .expiredAt(LocalDate.now().plusDays(30))
                .build();

        userCouponRepository.save(userCoupon1);

        // when
        GetUserCouponListUseCase.Input input = new GetUserCouponListUseCase.Input(userId);
        List<GetUserCouponListUseCase.Output> result = getUserCouponListUseCase.execute(input);

        // then
        assertThat(result).hasSizeGreaterThanOrEqualTo(1);
        assertThat(result).anyMatch(output ->
                output.userId() == userId &&
                        output.couponName().equals("신규 회원 할인 쿠폰") &&
                        output.userCouponStatus().equals("ISSUED")
        );
    }

    @Test
    @DisplayName("사용자가 발급받은 쿠폰이 없는 경우 빈 리스트 반환")
    void 사용자_쿠폰_목록_조회_빈_리스트() {
        // given
        long userId = 9999L; // 존재하지 않는 사용자

        // when
        GetUserCouponListUseCase.Input input = new GetUserCouponListUseCase.Input(userId);
        List<GetUserCouponListUseCase.Output> result = getUserCouponListUseCase.execute(input);

        // then
        assertThat(result).isEmpty();
    }
}
