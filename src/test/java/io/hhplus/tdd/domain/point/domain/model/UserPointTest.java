//package io.hhplus.tdd.domain.point.domain.model;
//
//import io.hhplus.tdd.domain.point.exception.PointRangeException;
//import org.assertj.core.api.Assertions;
//import org.junit.jupiter.api.Nested;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//class UserPointTest {
//
//    @Nested
//    class 포인트_사용{
//        @Test
//        void 포인트_사용_성공(){
//
//            // given
//            long balance = 1000L;
//            long usePoint = 100L;
//            UserPoint userPoint = UserPoint.builder()
//                    .id(1L)
//                    .balance(balance)
//                    .build();
//
//            // when
//            userPoint.usePoint(usePoint);
//
//            // then
//            Assertions.assertThat(userPoint.getBalance()).isEqualTo(balance-usePoint);
//        }
//
//        @Test
//        void 포인트_사용_실패_금액초과(){
//
//            // given
//            long balance = 1000L;
//            long usePoint = 10000L;
//            UserPoint userPoint = UserPoint.builder()
//                    .id(1L)
//                    .balance(balance)
//                    .build();
//
//            // when , then
//            Assertions.assertThatThrownBy(()->userPoint.usePoint(usePoint))
//                    .isInstanceOf(PointRangeException.class);
//            Assertions.assertThat(userPoint.getBalance()).isEqualTo(balance);
//        }
//    }
//
//    @Nested
//    class 포인트_충전{
//        @Test
//        void 포인트_충전_성공(){
//
//            // given
//            long balance = 1000L;
//            long chargePoint = 1000L;
//            UserPoint userPoint = UserPoint.builder()
//                    .id(1L)
//                    .balance(balance)
//                    .build();
//
//            // when
//            userPoint.chargePoint(chargePoint);
//
//            // then
//            Assertions.assertThat(userPoint.getBalance()).isEqualTo(balance+chargePoint);
//        }
//
//        @Test
//        void 포인트_사용_실패_금액초과(){
//
//            // given
//            long balance = 1000L;
//            long chargePoint = 100L;
//            UserPoint userPoint = UserPoint.builder()
//                    .id(1L)
//                    .balance(balance)
//                    .build();
//
//            // when , then
//            Assertions.assertThatThrownBy(()->userPoint.chargePoint(chargePoint))
//                    .isInstanceOf(PointRangeException.class);
//            Assertions.assertThat(userPoint.getBalance()).isEqualTo(balance);
//        }
//    }
//
//
//}