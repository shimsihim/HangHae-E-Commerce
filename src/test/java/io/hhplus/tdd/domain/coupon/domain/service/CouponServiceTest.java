package io.hhplus.tdd.domain.coupon.domain.service;

import io.hhplus.tdd.common.exception.ErrorCode;
import io.hhplus.tdd.domain.coupon.domain.model.Coupon;
import io.hhplus.tdd.domain.coupon.domain.model.DiscountType;
import io.hhplus.tdd.domain.coupon.domain.model.Status;
import io.hhplus.tdd.domain.coupon.domain.model.UserCoupon;
import io.hhplus.tdd.domain.coupon.exception.CouponException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("쿠폰 도메인 서비스 테스트")
class CouponServiceTest {

    private final CouponService couponService = new CouponService();

    @Nested
    @DisplayName("쿠폰 발급")
    class IssueCoupon {

        @Test
        @DisplayName("정상 발급")
        void 정상_발급() {
            // given
            long userId = 1L;
            Coupon coupon = Coupon.builder()
                    .id(1L)
                    .couponName("테스트 쿠폰")
                    .discountType(DiscountType.FIXED_AMOUNT)
                    .discountValue(5000)
                    .totalQuantity(100)
                    .issuedQuantity(0)
                    .limitPerUser(1)
                    .duration(30)
                    .minOrderValue(10000)
                    .validFrom(LocalDateTime.now().minusDays(1))
                    .validUntil(LocalDateTime.now().plusDays(30))
                    .build();

            List<UserCoupon> userIssuedCoupons = new ArrayList<>();

            // when
            UserCoupon result = couponService.issueCoupon(coupon, userId, userIssuedCoupons);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo(userId);
            assertThat(result.getCouponId()).isEqualTo(coupon.getId());
            assertThat(result.getStatus()).isEqualTo(Status.ISSUED);
            assertThat(coupon.getIssuedQuantity()).isEqualTo(1);
        }

        @Test
        @DisplayName("발급 기간 만료")
        void 발급_기간_만료() {
            // given
            long userId = 1L;
            Coupon coupon = Coupon.builder()
                    .id(1L)
                    .couponName("만료된 쿠폰")
                    .discountType(DiscountType.FIXED_AMOUNT)
                    .discountValue(5000)
                    .totalQuantity(100)
                    .issuedQuantity(0)
                    .limitPerUser(1)
                    .duration(30)
                    .minOrderValue(10000)
                    .validFrom(LocalDateTime.now().minusDays(10))
                    .validUntil(LocalDateTime.now().minusDays(1))  // 만료됨
                    .build();

            List<UserCoupon> userIssuedCoupons = new ArrayList<>();

            // when & then
            assertThatThrownBy(() -> couponService.issueCoupon(coupon, userId, userIssuedCoupons))
                    .isInstanceOf(CouponException.class);
        }

        @Test
        @DisplayName("전체 발급 수량 초과")
        void 전체_발급_수량_초과() {
            // given
            long userId = 1L;
            Coupon coupon = Coupon.builder()
                    .id(1L)
                    .couponName("테스트 쿠폰")
                    .discountType(DiscountType.FIXED_AMOUNT)
                    .discountValue(5000)
                    .totalQuantity(10)
                    .issuedQuantity(10)  // 이미 전체 발급 완료
                    .limitPerUser(1)
                    .duration(30)
                    .minOrderValue(10000)
                    .validFrom(LocalDateTime.now().minusDays(1))
                    .validUntil(LocalDateTime.now().plusDays(30))
                    .build();

            List<UserCoupon> userIssuedCoupons = new ArrayList<>();

            // when & then
            assertThatThrownBy(() -> couponService.issueCoupon(coupon, userId, userIssuedCoupons))
                    .isInstanceOf(CouponException.class);
        }

        @Test
        @DisplayName("사용자별 발급 제한 초과")
        void 사용자별_발급_제한_초과() {
            // given
            long userId = 1L;
            Coupon coupon = Coupon.builder()
                    .id(1L)
                    .couponName("테스트 쿠폰")
                    .discountType(DiscountType.FIXED_AMOUNT)
                    .discountValue(5000)
                    .totalQuantity(100)
                    .issuedQuantity(5)
                    .limitPerUser(1)  // 사용자당 1개만
                    .duration(30)
                    .minOrderValue(10000)
                    .validFrom(LocalDateTime.now().minusDays(1))
                    .validUntil(LocalDateTime.now().plusDays(30))
                    .build();

            // 이미 발급받은 쿠폰
            UserCoupon existingCoupon = UserCoupon.builder()
                    .id(1L)
                    .userId(userId)
                    .couponId(coupon.getId())
                    .status(Status.ISSUED)
                    .build();

            List<UserCoupon> userIssuedCoupons = List.of(existingCoupon);

            // when & then
            assertThatThrownBy(() -> couponService.issueCoupon(coupon, userId, userIssuedCoupons))
                    .isInstanceOf(CouponException.class);
        }
    }

    @Nested
    @DisplayName("할인 금액 계산")
    class ApplyDiscount {

        @ParameterizedTest(name = "정률 할인: 주문금액 {0}, 할인율 {1}% → 할인금액 {2}")
        @CsvSource({
                "10000, 10, 1000",
                "50000, 20, 10000",
                "100000, 15, 15000"
        })
        void 정률_할인_계산(long orderAmount, int discountRate, long expectedDiscount) {
            // given
            Coupon coupon = Coupon.builder()
                    .id(1L)
                    .discountType(DiscountType.PERCENTAGE)
                    .discountValue(discountRate)
                    .minOrderValue(5000)
                    .validFrom(LocalDateTime.now().minusDays(1))
                    .validUntil(LocalDateTime.now().plusDays(30))
                    .build();

            UserCoupon userCoupon = UserCoupon.builder()
                    .id(1L)
                    .userId(1L)
                    .couponId(coupon.getId())
                    .status(Status.ISSUED)
                    .expiredAt(LocalDateTime.now().plusDays(30))
                    .build();

            // when
            long discountAmount = couponService.applyDiscount(coupon, userCoupon, orderAmount);

            // then
            assertThat(discountAmount).isEqualTo(expectedDiscount);
            assertThat(userCoupon.getStatus()).isEqualTo(Status.USED);
        }

        @ParameterizedTest(name = "정액 할인: 주문금액 {0}, 할인금액 {1}")
        @CsvSource({
                "10000, 5000",
                "50000, 10000",
                "100000, 20000"
        })
        void 정액_할인_계산(long orderAmount, int discountValue) {
            // given
            Coupon coupon = Coupon.builder()
                    .id(1L)
                    .discountType(DiscountType.FIXED_AMOUNT)
                    .discountValue(discountValue)
                    .minOrderValue(5000)
                    .validFrom(LocalDateTime.now().minusDays(1))
                    .validUntil(LocalDateTime.now().plusDays(30))
                    .build();

            UserCoupon userCoupon = UserCoupon.builder()
                    .id(1L)
                    .userId(1L)
                    .couponId(coupon.getId())
                    .status(Status.ISSUED)
                    .expiredAt(LocalDateTime.now().plusDays(30))
                    .build();

            // when
            long discountAmount = couponService.applyDiscount(coupon, userCoupon, orderAmount);

            // then
            assertThat(discountAmount).isEqualTo(discountValue);
            assertThat(userCoupon.getStatus()).isEqualTo(Status.USED);
        }

        @Test
        @DisplayName("최소 주문 금액 미만")
        void 최소_주문_금액_미만() {
            // given
            Coupon coupon = Coupon.builder()
                    .id(1L)
                    .discountType(DiscountType.FIXED_AMOUNT)
                    .discountValue(5000)
                    .minOrderValue(10000)  // 최소 10000원
                    .validFrom(LocalDateTime.now().minusDays(1))
                    .validUntil(LocalDateTime.now().plusDays(30))
                    .build();

            UserCoupon userCoupon = UserCoupon.builder()
                    .id(1L)
                    .userId(1L)
                    .couponId(coupon.getId())
                    .status(Status.ISSUED)
                    .expiredAt(LocalDateTime.now().plusDays(30))
                    .build();

            // when & then
            assertThatThrownBy(() -> couponService.applyDiscount(coupon, userCoupon, 9000L))
                    .isInstanceOf(CouponException.class);
        }

        @Test
        @DisplayName("이미 사용된 쿠폰")
        void 이미_사용된_쿠폰() {
            // given
            Coupon coupon = Coupon.builder()
                    .id(1L)
                    .discountType(DiscountType.FIXED_AMOUNT)
                    .discountValue(5000)
                    .minOrderValue(5000)
                    .validFrom(LocalDateTime.now().minusDays(1))
                    .validUntil(LocalDateTime.now().plusDays(30))
                    .build();

            UserCoupon userCoupon = UserCoupon.builder()
                    .id(1L)
                    .userId(1L)
                    .couponId(coupon.getId())
                    .status(Status.USED)  // 이미 사용됨
                    .expiredAt(LocalDateTime.now().plusDays(30))
                    .build();

            // when & then
            assertThatThrownBy(() -> couponService.applyDiscount(coupon, userCoupon, 10000L))
                    .isInstanceOf(CouponException.class);
        }
    }

    @Nested
    @DisplayName("쿠폰 사용 취소")
    class CancelUsage {

        @Test
        @DisplayName("사용 취소 성공")
        void 사용_취소_성공() {
            // given
            UserCoupon userCoupon = UserCoupon.builder()
                    .id(1L)
                    .userId(1L)
                    .couponId(1L)
                    .status(Status.USED)
                    .build();

            // when
            couponService.cancelUsage(userCoupon);

            // then
            assertThat(userCoupon.getStatus()).isEqualTo(Status.ISSUED);
        }
    }

    @Nested
    @DisplayName("쿠폰 발급 취소")
    class CancelIssuance {

        @Test
        @DisplayName("발급 취소 성공")
        void 발급_취소_성공() {
            // given
            Coupon coupon = Coupon.builder()
                    .id(1L)
                    .couponName("테스트 쿠폰")
                    .discountType(DiscountType.FIXED_AMOUNT)
                    .discountValue(5000)
                    .totalQuantity(100)
                    .issuedQuantity(10)
                    .limitPerUser(1)
                    .duration(30)
                    .minOrderValue(10000)
                    .validFrom(LocalDateTime.now().minusDays(1))
                    .validUntil(LocalDateTime.now().plusDays(30))
                    .build();

            // when
            couponService.cancelIssuance(coupon);

            // then
            assertThat(coupon.getIssuedQuantity()).isEqualTo(9);
        }
    }
}
