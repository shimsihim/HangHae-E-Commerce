//package io.hhplus.tdd.domain.coupon.domain.service;
//
//import io.hhplus.tdd.domain.coupon.domain.model.Coupon;
//import io.hhplus.tdd.domain.coupon.domain.model.DiscountType;
//import io.hhplus.tdd.domain.coupon.domain.model.Status;
//import io.hhplus.tdd.domain.coupon.domain.model.UserCoupon;
//import io.hhplus.tdd.domain.coupon.exception.CouponException;
//import org.junit.jupiter.api.Nested;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.params.ParameterizedTest;
//import org.junit.jupiter.params.provider.CsvSource;
//
//import java.time.LocalDate;
//import java.util.ArrayList;
//import java.util.List;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.assertj.core.api.Assertions.assertThatThrownBy;
//
//class CouponServiceTest {
//
//    private final CouponService couponService = new CouponService();
//
//    @Nested
//    class 쿠폰_발급 {
//
//        @Test
//        void 정상_발급() {
//            // given
//            long userId = 1L;
//            Coupon coupon = Coupon.builder()
//                    .id(1L)
//                    .couponName("테스트")
//                    .discountType(DiscountType.FIXED_AMOUNT)
//                    .discountValue(5000)
//                    .totalQuantity(100)
//                    .issuedQuantity(0)
//                    .limitPerUser(1)
//                    .duration(30)
//                    .minOrderValue(10000)
//                    .validFrom(LocalDate.now().minusDays(1))
//                    .validUntil(LocalDate.now().plusDays(30))
//                    .build();
//
//            List<UserCoupon> userIssuedCoupons = new ArrayList<>();
//
//            // when
//            UserCoupon result = couponService.issueCoupon(coupon, userId, userIssuedCoupons);
//
//            // then
//            assertThat(result).isNotNull();
//            assertThat(result.getUserId()).isEqualTo(userId);
//            assertThat(result.getCouponId()).isEqualTo(coupon.getId());
//            assertThat(result.getStatus()).isEqualTo(Status.ISSUED);
//            assertThat(coupon.getIssuedQuantity()).isEqualTo(1);
//        }
//
//        @Test
//        void 발급_기간_만료() {
//            // given
//            long userId = 1L;
//            Coupon coupon = Coupon.builder()
//                    .id(1L)
//                    .couponName("test")
//                    .discountType(DiscountType.FIXED_AMOUNT)
//                    .discountValue(5000)
//                    .totalQuantity(100)
//                    .issuedQuantity(0)
//                    .limitPerUser(1)
//                    .duration(30)
//                    .minOrderValue(10000)
//                    .validFrom(LocalDate.now().minusDays(10))
//                    .validUntil(LocalDate.now().minusDays(1))
//                    .build();
//
//            List<UserCoupon> userIssuedCoupons = new ArrayList<>();
//
//            // when , then
//            assertThatThrownBy(() -> couponService.issueCoupon(coupon, userId, userIssuedCoupons))
//                    .isInstanceOf(CouponException.class);
//        }
//
//        @Test
//        void 전체_발급_수량_초과() {
//            // given
//            long userId = 1L;
//            Coupon coupon = Coupon.builder()
//                    .id(1L)
//                    .couponName("test")
//                    .discountType(DiscountType.FIXED_AMOUNT)
//                    .discountValue(5000)
//                    .totalQuantity(10)
//                    .issuedQuantity(10)
//                    .limitPerUser(1)
//                    .duration(30)
//                    .minOrderValue(10000)
//                    .validFrom(LocalDate.now().minusDays(1))
//                    .validUntil(LocalDate.now().plusDays(30))
//                    .build();
//
//            List<UserCoupon> userIssuedCoupons = new ArrayList<>();
//
//            // when , then
//            assertThatThrownBy(() -> couponService.issueCoupon(coupon, userId, userIssuedCoupons))
//                    .isInstanceOf(CouponException.class);
//        }
//
//        @Test
//        void 사용자별_발급_제한_초과() {
//            // given
//            long userId = 1L;
//            Coupon coupon = Coupon.builder()
//                    .id(1L)
//                    .couponName("test")
//                    .discountType(DiscountType.FIXED_AMOUNT)
//                    .discountValue(5000)
//                    .totalQuantity(100)
//                    .issuedQuantity(5)
//                    .limitPerUser(1)
//                    .duration(30)
//                    .minOrderValue(10000)
//                    .validFrom(LocalDate.now().minusDays(1))
//                    .validUntil(LocalDate.now().plusDays(30))
//                    .build();
//
//            // 발급받은 쿠폰
//            UserCoupon existingCoupon = UserCoupon.builder()
//                    .id(1L)
//                    .userId(userId)
//                    .couponId(coupon.getId())
//                    .status(Status.ISSUED)
//                    .build();
//
//            List<UserCoupon> userIssuedCoupons = List.of(existingCoupon);
//
//            // when , then
//            assertThatThrownBy(() -> couponService.issueCoupon(coupon, userId, userIssuedCoupons))
//                    .isInstanceOf(CouponException.class);
//        }
//    }
//
//    @Nested
//    class ApplyDiscount {
//
//        @ParameterizedTest(name = "퍼센트 할인: 주문금액 {0}, 할인율 {1}% → 할인금액 {2}")
//        @CsvSource({
//                "10000, 10, 1000",
//                "50000, 20, 10000",
//                "100000, 15, 15000"
//        })
//        void 퍼센트_할인(long orderAmount, int discountRate, long expectedDiscount) {
//            // given
//            Coupon coupon = Coupon.builder()
//                    .id(1L)
//                    .discountType(DiscountType.PERCENTAGE)
//                    .discountValue(discountRate)
//                    .minOrderValue(5000)
//                    .validFrom(LocalDate.now().minusDays(1))
//                    .validUntil(LocalDate.now().plusDays(30))
//                    .build();
//
//            UserCoupon userCoupon = UserCoupon.builder()
//                    .id(1L)
//                    .userId(1L)
//                    .couponId(coupon.getId())
//                    .status(Status.ISSUED)
//                    .expiredAt(LocalDate.now().plusDays(30))
//                    .build();
//
//            // when
//            long discountAmount = couponService.useUserCoupon(coupon, userCoupon, orderAmount);
//
//            // then
//            assertThat(discountAmount).isEqualTo(expectedDiscount);
//            assertThat(userCoupon.getStatus()).isEqualTo(Status.USED);
//        }
//
//        @ParameterizedTest(name = "정액 할인: 주문금액 {0}, 할인금액 {1}")
//        @CsvSource({
//                "10000, 5000",
//                "50000, 10000",
//                "100000, 20000"
//        })
//        void 정액_할인_계산(long orderAmount, int discountValue) {
//            // given
//            Coupon coupon = Coupon.builder()
//                    .id(1L)
//                    .discountType(DiscountType.FIXED_AMOUNT)
//                    .discountValue(discountValue)
//                    .minOrderValue(5000)
//                    .validFrom(LocalDate.now().minusDays(1))
//                    .validUntil(LocalDate.now().plusDays(30))
//                    .build();
//
//            UserCoupon userCoupon = UserCoupon.builder()
//                    .id(1L)
//                    .userId(1L)
//                    .couponId(coupon.getId())
//                    .status(Status.ISSUED)
//                    .expiredAt(LocalDate.now().plusDays(30))
//                    .build();
//
//            // when
//            long discountAmount = couponService.useUserCoupon(coupon, userCoupon, orderAmount);
//
//            // then
//            assertThat(discountAmount).isEqualTo(discountValue);
//            assertThat(userCoupon.getStatus()).isEqualTo(Status.USED);
//        }
//
//        @Test
//        void 최소_주문_금액_미만() {
//            // given
//            int minOrderValue = 10000;
//            Coupon coupon = Coupon.builder()
//                    .id(1L)
//                    .discountType(DiscountType.FIXED_AMOUNT)
//                    .discountValue(5000)
//                    .minOrderValue(minOrderValue)
//                    .validFrom(LocalDate.now().minusDays(1))
//                    .validUntil(LocalDate.now().plusDays(30))
//                    .build();
//
//            UserCoupon userCoupon = UserCoupon.builder()
//                    .id(1L)
//                    .userId(1L)
//                    .couponId(coupon.getId())
//                    .status(Status.ISSUED)
//                    .expiredAt(LocalDate.now().plusDays(30))
//                    .build();
//
//            // when , then
//            assertThatThrownBy(() -> couponService.useUserCoupon(coupon, userCoupon, minOrderValue-1000))
//                    .isInstanceOf(CouponException.class);
//        }
//
//        @Test
//        void 이미_사용된_쿠폰() {
//            // given
//            Coupon coupon = Coupon.builder()
//                    .id(1L)
//                    .discountType(DiscountType.FIXED_AMOUNT)
//                    .discountValue(5000)
//                    .minOrderValue(5000)
//                    .validFrom(LocalDate.now().minusDays(1))
//                    .validUntil(LocalDate.now().plusDays(30))
//                    .build();
//
//            UserCoupon userCoupon = UserCoupon.builder()
//                    .id(1L)
//                    .userId(1L)
//                    .couponId(coupon.getId())
//                    .status(Status.USED)
//                    .expiredAt(LocalDate.now().plusDays(30))
//                    .build();
//
//            // when , then
//            assertThatThrownBy(() -> couponService.useUserCoupon(coupon, userCoupon, 10000L))
//                    .isInstanceOf(CouponException.class);
//        }
//    }
//
//}
