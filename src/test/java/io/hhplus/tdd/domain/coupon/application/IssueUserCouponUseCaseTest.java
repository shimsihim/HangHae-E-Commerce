package io.hhplus.tdd.domain.coupon.application;

import io.hhplus.tdd.common.exception.ErrorCode;
import io.hhplus.tdd.domain.coupon.domain.model.Coupon;
import io.hhplus.tdd.domain.coupon.domain.model.DiscountType;
import io.hhplus.tdd.domain.coupon.domain.model.Status;
import io.hhplus.tdd.domain.coupon.domain.model.UserCoupon;
import io.hhplus.tdd.domain.coupon.infrastructure.repository.CouponRepository;
import io.hhplus.tdd.domain.coupon.infrastructure.repository.UserCouponRepository;
import io.hhplus.tdd.domain.coupon.domain.service.CouponService;
import io.hhplus.tdd.domain.coupon.exception.CouponException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IssueUserCouponUseCaseTest {

    @InjectMocks
    IssueUserCouponUseCase issueUserCouponUseCase;

    @Mock
    CouponRepository couponRepository;

    @Mock
    UserCouponRepository userCouponRepository;

    @Mock
    CouponService couponService;

    @Nested
    class IssueSuccess {

        @Test
        void 정상_쿠폰_발급() {
            // given
            long couponId = 1L;
            long userId = 100L;

            Coupon coupon = Coupon.builder()
                    .id(couponId)
                    .couponName("테스트")
                    .discountType(DiscountType.FIXED_AMOUNT)
                    .discountValue(5000)
                    .totalQuantity(100)
                    .issuedQuantity(10)
                    .limitPerUser(1)
                    .duration(30)
                    .minOrderValue(10000)
                    .validFrom(LocalDate.now().minusDays(1))
                    .validUntil(LocalDate.now().plusDays(30))
                    .build();

            UserCoupon userCoupon = UserCoupon.builder()
                    .id(1L)
                    .userId(userId)
                    .couponId(couponId)
                    .status(Status.ISSUED)
                    .expiredAt(LocalDate.now().plusDays(30))
                    .build();

            given(couponRepository.findById(couponId)).willReturn(Optional.of(coupon));
            given(userCouponRepository.findByUserIdAndCouponId(userId, couponId)).willReturn(new ArrayList<>());
            given(couponService.issueCoupon(eq(coupon) , eq(userId), anyList())).willReturn(userCoupon);
            given(couponRepository.save(coupon)).willReturn(coupon);
            given(userCouponRepository.save(userCoupon)).willReturn(userCoupon);

            // when
            IssueUserCouponUseCase.Input input = new IssueUserCouponUseCase.Input(couponId, userId);
            issueUserCouponUseCase.execute(input);

            // then
            verify(couponRepository).findById(couponId);
            verify(couponService).issueCoupon(eq(coupon), eq(userId), anyList());
            verify(couponRepository).save(coupon);
            verify(userCouponRepository).save(any(UserCoupon.class));
        }
    }

    @Nested
    class 발급_씰패 {

        @Test
        void 존재하지_않는_쿠폰() {
            // given
            long couponId = 999L;
            long userId = 100L;

            given(couponRepository.findById(couponId)).willReturn(Optional.empty());


            IssueUserCouponUseCase.Input input = new IssueUserCouponUseCase.Input(couponId, userId);
            assertThatThrownBy(() ->
                    // when
                    issueUserCouponUseCase.execute(input)
            )
                    // then
                    .isInstanceOf(CouponException.class);
            //then
            verify(couponRepository).findById(couponId);
            verify(couponService, never()).issueCoupon(any(), anyLong(), anyList());
        }

        @Test
        void 사용자_발급_제한_초과() {
            // given
            long couponId = 1L;
            long userId = 100L;

            Coupon coupon = Coupon.builder()
                    .id(couponId)
                    .couponName("테스트 쿠폰")
                    .discountType(DiscountType.FIXED_AMOUNT)
                    .discountValue(5000)
                    .totalQuantity(100)
                    .issuedQuantity(10)
                    .limitPerUser(1)  // 사용자당 1개만 발급 가능
                    .duration(30)
                    .minOrderValue(10000)
                    .validFrom(LocalDate.now().minusDays(1))
                    .validUntil(LocalDate.now().plusDays(30))
                    .build();

            // 이미 발급받은 쿠폰
            UserCoupon existingCoupon = UserCoupon.builder()
                    .id(1L)
                    .userId(userId)
                    .couponId(couponId)
                    .status(Status.ISSUED)
                    .build();

            given(couponRepository.findById(couponId)).willReturn(Optional.of(coupon));
            given(userCouponRepository.findByUserIdAndCouponId(userId, couponId))
                    .willReturn(List.of(existingCoupon));
            given(couponService.issueCoupon(any(Coupon.class), eq(userId), anyList()))
                    .willThrow(new CouponException(ErrorCode.COUPON_ISSUE_LIMIT_PER_USER, couponId));


            IssueUserCouponUseCase.Input input = new IssueUserCouponUseCase.Input(couponId, userId);
            assertThatThrownBy(() ->
                    // when
                    issueUserCouponUseCase.execute(input))
                    // then
                    .isInstanceOf(CouponException.class);
        }

        @Test
        void 쿠폰_발급_수량_초과() {
            // given
            long couponId = 1L;
            long userId = 100L;

            Coupon coupon = Coupon.builder()
                    .id(couponId)
                    .couponName("test")
                    .discountType(DiscountType.FIXED_AMOUNT)
                    .discountValue(5000)
                    .totalQuantity(10)
                    .issuedQuantity(10)
                    .limitPerUser(1)
                    .duration(30)
                    .minOrderValue(10000)
                    .validFrom(LocalDate.now().minusDays(1))
                    .validUntil(LocalDate.now().plusDays(30))
                    .build();

            given(couponRepository.findById(couponId)).willReturn(Optional.of(coupon));
            IssueUserCouponUseCase.Input input = new IssueUserCouponUseCase.Input(couponId, userId);
            // when
            assertThatThrownBy(() -> issueUserCouponUseCase.execute(input))
                    // then
                    .isInstanceOf(CouponException.class);
        }
    }
}
