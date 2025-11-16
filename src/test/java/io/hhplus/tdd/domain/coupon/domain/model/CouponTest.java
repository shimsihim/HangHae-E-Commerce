//package io.hhplus.tdd.domain.coupon.domain.model;
//
//import io.hhplus.tdd.common.exception.ErrorCode;
//import io.hhplus.tdd.domain.coupon.exception.CouponException;
//import org.assertj.core.api.Assertions;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Nested;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.time.LocalDate;
//import java.time.LocalDate;
//
//import static org.assertj.core.api.Assertions.*;
//import static org.junit.jupiter.api.Assertions.*;
//
//@ExtendWith(MockitoExtension.class)
//class CouponTest {
//
//    @Nested
//    class 정액_할인_계산{
//        @Test
//        void 쿠폰_정액_할인율_계산_성공(){
//            //given
//            DiscountType discountType = DiscountType.FIXED_AMOUNT;
//            int discountAmount = 1000;
//            long orderAmount = 1000L;
//            int minOrderValue = 1000;
//
//            Coupon coupon = Coupon.builder()
//                    .id(1L)
//                    .couponName("테스트")
//                    .discountType(discountType)
//                    .discountValue(discountAmount)
//                    .totalQuantity(100)
//                    .issuedQuantity(10)
//                    .limitPerUser(1)
//                    .duration(30)
//                    .minOrderValue(minOrderValue)
//                    .validFrom(LocalDate.now().minusDays(1))
//                    .validUntil(LocalDate.now().plusDays(30))
//                    .build();
//            //when
//            long discountValue = coupon.calculateDiscountAmount(orderAmount);
//
//            //then
//            assertThat(discountValue).isEqualTo(discountAmount);
//        }
//
//        @Test
//        void 쿠폰_비율_할인율_계산_성공(){
//            //given
//            DiscountType discountType = DiscountType.PERCENTAGE;
//            int discountAmount = 10;
//            long orderAmount = 10000;
//            int minOrderValue = 1000;
//
//            Coupon coupon = Coupon.builder()
//                    .id(1L)
//                    .couponName("테스트")
//                    .discountType(discountType)
//                    .discountValue(discountAmount)
//                    .totalQuantity(100)
//                    .issuedQuantity(10)
//                    .limitPerUser(1)
//                    .duration(30)
//                    .minOrderValue(minOrderValue)
//                    .validFrom(LocalDate.now().minusDays(1))
//                    .validUntil(LocalDate.now().plusDays(30))
//                    .build();
//            //when
//            long discountValue = coupon.calculateDiscountAmount(orderAmount);
//
//            //then
//            assertThat(discountValue).isEqualTo(orderAmount * (discountAmount)/100);
//        }
//
//        @Test
//        void 최소주문금액_미만_주문_할인율(){
//            //given
//            DiscountType discountType = DiscountType.FIXED_AMOUNT;
//            int discountAmount = 1000;
//            long orderAmount = 1000L;
//            int minOrderValue = 10000;
//
//            Coupon coupon = Coupon.builder()
//                    .id(1L)
//                    .couponName("테스트")
//                    .discountType(DiscountType.FIXED_AMOUNT)
//                    .discountValue(discountAmount)
//                    .totalQuantity(100)
//                    .issuedQuantity(10)
//                    .limitPerUser(1)
//                    .duration(30)
//                    .minOrderValue(minOrderValue)
//                    .validFrom(LocalDate.now().minusDays(1))
//                    .validUntil(LocalDate.now().plusDays(30))
//                    .build();
//            //when
//            long discountValue = coupon.calculateDiscountAmount(orderAmount);
//
//            //then
//            assertThat(discountValue).isEqualTo(0);
//        }
//    }
//
//    @Nested
//    class 쿠폰_발급_유효성{
//
//        @Test
//        void 발급_유효성_실패_테스트_시작날짜_미도래(){
//
//            //given
//            LocalDate validFrom = LocalDate.now().plusDays(1);
//            LocalDate validEnd = LocalDate.now().plusDays(10);
//
//            Coupon coupon = Coupon.builder()
//                    .id(1L)
//                    .couponName("테스트")
//                    .discountType(DiscountType.PERCENTAGE)
//                    .discountValue(10)
//                    .totalQuantity(100)
//                    .issuedQuantity(10)
//                    .limitPerUser(1)
//                    .duration(30)
//                    .minOrderValue(1000)
//                    .validFrom(validFrom)
//                    .validUntil(validEnd)
//                    .build();
//            //when
//            CouponException ex = assertThrows(CouponException.class , ()->coupon.validIssue());
//            assertThat(ex.getErrCode()).isEqualTo(ErrorCode.COUPON_DURATION_ERR);
//        }
//
//        @Test
//        void 발급_유효성_실패_테스트_종료날짜_지남(){
//
//            //given
//            LocalDate validFrom = LocalDate.now().minusDays(10);
//            LocalDate validEnd = LocalDate.now().minusDays(1);
//
//            Coupon coupon = Coupon.builder()
//                    .id(1L)
//                    .couponName("테스트")
//                    .discountType(DiscountType.PERCENTAGE)
//                    .discountValue(10)
//                    .totalQuantity(100)
//                    .issuedQuantity(10)
//                    .limitPerUser(1)
//                    .duration(30)
//                    .minOrderValue(1000)
//                    .validFrom(validFrom)
//                    .validUntil(validEnd)
//                    .build();
//            //when
//            CouponException ex = assertThrows(CouponException.class , ()->coupon.validIssue());
//            assertThat(ex.getErrCode()).isEqualTo(ErrorCode.COUPON_DURATION_ERR);
//        }
//
//        @Test
//        void 발급_유효성_실패_테스트_쿠폰_수량_없음(){
//
//            //given
//            LocalDate validFrom = LocalDate.now();
//            LocalDate validEnd = LocalDate.now();
//
//            Coupon coupon = Coupon.builder()
//                    .id(1L)
//                    .couponName("테스트")
//                    .discountType(DiscountType.PERCENTAGE)
//                    .discountValue(10)
//                    .totalQuantity(100)
//                    .issuedQuantity(100)
//                    .limitPerUser(1)
//                    .duration(30)
//                    .minOrderValue(1000)
//                    .validFrom(validFrom)
//                    .validUntil(validEnd)
//                    .build();
//            //when
//            CouponException ex = assertThrows(CouponException.class , ()->coupon.validIssue());
//            assertThat(ex.getErrCode()).isEqualTo(ErrorCode.COUPON_ISSUE_LIMIT);
//        }
//
//        @Test
//        void 발급_유효성_성공(){
//
//            //given
//            LocalDate validFrom = LocalDate.now();
//            LocalDate validEnd = LocalDate.now();
//
//            Coupon coupon = Coupon.builder()
//                    .id(1L)
//                    .couponName("테스트")
//                    .discountType(DiscountType.PERCENTAGE)
//                    .discountValue(10)
//                    .totalQuantity(100)
//                    .issuedQuantity(50)
//                    .limitPerUser(1)
//                    .duration(30)
//                    .minOrderValue(1000)
//                    .validFrom(validFrom)
//                    .validUntil(validEnd)
//                    .build();
//            //when
//            assertDoesNotThrow(()->coupon.validIssue());
//        }
//    }
//}