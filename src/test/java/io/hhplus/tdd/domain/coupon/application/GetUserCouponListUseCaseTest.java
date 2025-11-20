package io.hhplus.tdd.domain.coupon.application;

import io.hhplus.tdd.domain.coupon.domain.model.Coupon;
import io.hhplus.tdd.domain.coupon.domain.model.DiscountType;
import io.hhplus.tdd.domain.coupon.domain.model.Status;
import io.hhplus.tdd.domain.coupon.domain.model.UserCoupon;
import io.hhplus.tdd.domain.coupon.infrastructure.repository.CouponRepository;
import io.hhplus.tdd.domain.coupon.infrastructure.repository.UserCouponRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GetUserCouponListUseCaseTest {

    @InjectMocks
    GetUserCouponListUseCase getUserCouponListUseCase;

    @Mock
    UserCouponRepository userCouponRepository;

    @Mock
    CouponRepository couponRepository;

    @Test
    void 사용자_쿠폰_목록_조회_성공() {
        // given
        long userId = 1L;

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

        UserCoupon userCoupon1 = UserCoupon.builder()
                .id(1L)
                .userId(userId)
                .couponId(1L)
                .coupon(coupon1)
                .status(Status.ISSUED)
                .expiredAt(LocalDate.now().plusDays(30))
                .build();

        UserCoupon userCoupon2 = UserCoupon.builder()
                .id(2L)
                .userId(userId)
                .couponId(2L)
                .coupon(coupon2)
                .status(Status.USED)
                .usedAt(LocalDate.now().minusDays(5))
                .expiredAt(LocalDate.now().plusDays(60))
                .build();

        List<UserCoupon> userCoupons = Arrays.asList(userCoupon1, userCoupon2);
        given(userCouponRepository.findByUserId(userId)).willReturn(userCoupons);
        given(couponRepository.findById(1L)).willReturn(Optional.of(coupon1));
        given(couponRepository.findById(2L)).willReturn(Optional.of(coupon2));

        // when
        GetUserCouponListUseCase.Input input = new GetUserCouponListUseCase.Input(userId);
        List<GetUserCouponListUseCase.Output> result = getUserCouponListUseCase.execute(input);

        // then
        assertThat(result).hasSize(2);

        assertThat(result.get(0).id()).isEqualTo(1L);
        assertThat(result.get(0).userId()).isEqualTo(userId);
        assertThat(result.get(0).couponName()).isEqualTo("신규 회원 할인 쿠폰");
        assertThat(result.get(0).userCouponStatus()).isEqualTo("ISSUED");

        assertThat(result.get(1).id()).isEqualTo(2L);
        assertThat(result.get(1).userId()).isEqualTo(userId);
        assertThat(result.get(1).couponName()).isEqualTo("VIP 회원 할인 쿠폰");
        assertThat(result.get(1).userCouponStatus()).isEqualTo("USED");

        verify(userCouponRepository).findByUserId(userId);
        verify(couponRepository).findById(1L);
        verify(couponRepository).findById(2L);
    }

    @Test
    void 사용자가_발급받은_쿠폰이_없는_경우_빈_리스트_반환() {
        // given
        long userId = 1L;
        given(userCouponRepository.findByUserId(userId)).willReturn(Collections.emptyList());

        // when
        GetUserCouponListUseCase.Input input = new GetUserCouponListUseCase.Input(userId);
        List<GetUserCouponListUseCase.Output> result = getUserCouponListUseCase.execute(input);

        // then
        assertThat(result).isEmpty();
        verify(userCouponRepository).findByUserId(userId);
    }
}
