package io.hhplus.tdd.domain.coupon.application;

import io.hhplus.tdd.domain.coupon.domain.model.Coupon;
import io.hhplus.tdd.domain.coupon.domain.model.DiscountType;
import io.hhplus.tdd.domain.coupon.infrastructure.repository.CouponRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GetAllCouponListUseCaseTest {

    @InjectMocks
    GetAllCouponListUseCase getAllCouponListUseCase;

    @Mock
    CouponRepository couponRepository;

    @Test
    void 쿠폰_목록_조회_성공() {
        // given
        Coupon coupon1 = Coupon.builder()
                .id(1L)
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
                .id(2L)
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

        List<Coupon> coupons = Arrays.asList(coupon1, coupon2);
        given(couponRepository.findAll()).willReturn(coupons);

        // when
        List<GetAllCouponListUseCase.Output> result = getAllCouponListUseCase.execute();

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).id()).isEqualTo(1L);
        assertThat(result.get(0).couponName()).isEqualTo("신규 회원 할인 쿠폰");
        assertThat(result.get(0).discountType()).isEqualTo("PERCENTAGE");
        assertThat(result.get(0).discountValue()).isEqualTo(10);

        assertThat(result.get(1).id()).isEqualTo(2L);
        assertThat(result.get(1).couponName()).isEqualTo("VIP 회원 할인 쿠폰");
        assertThat(result.get(1).discountType()).isEqualTo("FIXED_AMOUNT");
        assertThat(result.get(1).discountValue()).isEqualTo(5000);

        verify(couponRepository).findAll();
    }

    @Test
    void 쿠폰이_없는_경우_빈_리스트_반환() {
        // given
        given(couponRepository.findAll()).willReturn(Collections.emptyList());

        // when
        List<GetAllCouponListUseCase.Output> result = getAllCouponListUseCase.execute();

        // then
        assertThat(result).isEmpty();
        verify(couponRepository).findAll();
    }
}
