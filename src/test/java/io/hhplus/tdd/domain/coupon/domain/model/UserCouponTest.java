package io.hhplus.tdd.domain.coupon.domain.model;

import io.hhplus.tdd.common.exception.ErrorCode;
import io.hhplus.tdd.domain.coupon.exception.CouponException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class UserCouponTest {

    @Test
    void 쿠폰_사용_테스트_정상(){

        // given
        UserCoupon userCoupon = UserCoupon.builder()
                .id(1L)
                .userId(1L)
                .couponId(1L)
                .status(Status.ISSUED)
                .expiredAt(LocalDate.now().plusDays(30))
                .build();

        // when , then
        assertDoesNotThrow(()->userCoupon.useCoupon());
    }

    @Test
    void 쿠폰_사용_테스트_실패_지난_날짜(){

        // given
        UserCoupon userCoupon = UserCoupon.builder()
                .id(1L)
                .userId(1L)
                .couponId(1L)
                .status(Status.ISSUED)
                .expiredAt(LocalDate.now().minusDays(1))
                .build();

        // when , then
        CouponException ex = assertThrows(CouponException.class , ()->userCoupon.useCoupon());

        // then
        assertThat(ex.getErrCode()).isEqualTo(ErrorCode.COUPON_USER_EXPIRED);
    }

    @Test
    void 쿠폰_사용_테스트_실패_이미_사용(){

        // given
        UserCoupon userCoupon = UserCoupon.builder()
                .id(1L)
                .userId(1L)
                .couponId(1L)
                .status(Status.USED)
                .expiredAt(LocalDate.now())
                .build();

        // when , then
        CouponException ex = assertThrows(CouponException.class , ()->userCoupon.useCoupon());

        // then
        assertThat(ex.getErrCode()).isEqualTo(ErrorCode.COUPON_USER_USED);
    }
}