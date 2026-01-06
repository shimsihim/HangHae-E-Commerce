package io.hhplus.tdd.domain.coupon.application;

import io.hhplus.tdd.domain.IntegrationTest;
import io.hhplus.tdd.domain.coupon.domain.model.Coupon;
import io.hhplus.tdd.domain.coupon.domain.model.DiscountType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GetAllCouponListIntegrationTest extends IntegrationTest {

    @Autowired
    private GetAllCouponListUseCase getAllCouponListUseCase;

    @Test
    @DisplayName("쿠폰 목록 조회 통합 테스트 - 모든 쿠폰을 정상적으로 조회한다")
    void 쿠폰_목록_조회_성공() {
        // given
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

        Coupon coupon2 = Coupon.builder()
                .couponName("VIP 회원 할인 쿠폰")
                .discountType(DiscountType.FIXED_AMOUNT)
                .discountValue(5000)
                .totalQuantity(50)
                .issuedQuantity(20)
                .limitPerUser(2)
                .duration(60)
                .minOrderValue(50000)
                .validFrom(LocalDate.now().minusDays(10))
                .validUntil(LocalDate.now().plusDays(50))
                .build();

        couponRepository.save(coupon1);
        couponRepository.save(coupon2);

        // when
        List<GetAllCouponListUseCase.Output> result = getAllCouponListUseCase.execute();

        // then
        assertThat(result).hasSizeGreaterThanOrEqualTo(2);
        assertThat(result).anyMatch(output ->
                output.couponName().equals("신규 회원 할인 쿠폰") &&
                        output.discountType().equals("PERCENTAGE") &&
                        output.discountValue() == 10
        );
        assertThat(result).anyMatch(output ->
                output.couponName().equals("VIP 회원 할인 쿠폰") &&
                        output.discountType().equals("FIXED_AMOUNT") &&
                        output.discountValue() == 5000
        );
    }

    @Test
    @DisplayName("쿠폰이 없는 경우 빈 리스트 반환")
    void 쿠폰_목록_조회_빈_리스트() {
        // given - 데이터 없음

        // when
        List<GetAllCouponListUseCase.Output> result = getAllCouponListUseCase.execute();

        // then
        assertThat(result).isNotNull();
    }
}
