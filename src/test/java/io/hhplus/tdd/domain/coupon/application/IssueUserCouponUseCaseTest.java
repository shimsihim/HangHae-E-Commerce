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
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.mock.mockito.SpyBean;

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

    @Spy// 실제 인스턴스를 생성
    CouponService couponService;

    @Captor
    private ArgumentCaptor<UserCoupon> userCouponArgumentCaptor;

    private final int duration = 30;
    private Coupon.CouponBuilder testNormalCoupon = Coupon.builder()
            .id(1l)
            .couponName("테스트")
            .discountType(DiscountType.FIXED_AMOUNT)
            .discountValue(5000)
            .totalQuantity(200)
            .issuedQuantity(10)
            .limitPerUser(1)
            .duration(duration)
            .minOrderValue(10000)
            .validFrom(LocalDate.now().minusDays(1))
            .validUntil(LocalDate.now().plusDays(30));

    private UserCoupon.UserCouponBuilder testNormalUserCoupon = UserCoupon.builder()
            .id(1L)
            .userId(1l)
            .couponId(1l)
            .status(Status.ISSUED)
            .expiredAt(LocalDate.now().plusDays(duration));

    @Nested
    class IssueSuccess {

        @Test
        void 정상_쿠폰_발급() {
            // given
            Coupon coupon = testNormalCoupon.build();
            UserCoupon userCoupon = testNormalUserCoupon.coupon(coupon).couponId(coupon.getId()).expiredAt(LocalDate.now().plusDays(coupon.getDuration())).build();

            long couponId = coupon.getId();
            long userId = userCoupon.getUserId();

            given(couponRepository.findForPessimisticById(couponId)).willReturn(Optional.of(coupon));
            given(userCouponRepository.findByUserIdAndCouponId(userId, couponId)).willReturn(new ArrayList<>());

            // when
            IssueUserCouponUseCase.Input input = new IssueUserCouponUseCase.Input(coupon.getId(), userId);
            issueUserCouponUseCase.execute(input);

            // then
            verify(couponRepository).findForPessimisticById(eq(couponId));
            verify(couponService).issueCoupon(eq(coupon), eq(userId), anyList());
            verify(couponRepository).save(eq(coupon));
            verify(userCouponRepository).save(userCouponArgumentCaptor.capture());
            UserCoupon capturedUserCoupon = userCouponArgumentCaptor.getValue();
            assertThat(capturedUserCoupon)
                    .usingRecursiveComparison()
                    .ignoringFields("id") // auto-increment되는 필드는 무시
                    .isEqualTo(userCoupon);


            //호출 순서 검증
            // Then (순서 검증)

            // 1. 순서 검증을 위한 InOrder 객체를 생성합니다.
            // 검증할 모든 Mock 객체를 인자로 넘깁니다.
            InOrder inOrder = inOrder(couponRepository, userCouponRepository, couponService , couponRepository , userCouponRepository);

            // 2. inOrder 객체를 사용하여 순서대로 verify를 수행합니다.
            // 만약 이 순서가 실제 호출 순서와 다르다면 테스트는 실패합니다.
            inOrder.verify(couponRepository).findForPessimisticById(eq(couponId));
            inOrder.verify(userCouponRepository).findByUserIdAndCouponId(userId, couponId);
            inOrder.verify(couponService).issueCoupon(eq(coupon), eq(userId) , anyList());
            inOrder.verify(couponRepository).save(any());
            inOrder.verify(userCouponRepository).save(any());
        }
    }

    @Nested
    class 발급_씰패 {

        @Test
        void 존재하지_않는_쿠폰() {
            // given

            Coupon coupon = testNormalCoupon.build();
            UserCoupon userCoupon = testNormalUserCoupon.coupon(coupon).couponId(coupon.getId()).expiredAt(LocalDate.now().plusDays(coupon.getDuration())).build();

            long couponId = coupon.getId();
            long userId = userCoupon.getUserId();

            given(couponRepository.findForPessimisticById(couponId)).willReturn(Optional.empty());


            IssueUserCouponUseCase.Input input = new IssueUserCouponUseCase.Input(couponId, userId);
            assertThatThrownBy(() ->
                    // when
                    issueUserCouponUseCase.execute(input)
            )
                    // then
                    .isInstanceOf(CouponException.class);
            //then
            verify(userCouponRepository , never()).findByUserIdAndCouponId(anyLong(), anyLong());
            verify(couponService, never()).issueCoupon(any(), anyLong(), anyList());
            verify(couponRepository , never()).save(any());
            verify(userCouponRepository , never()).save(any());
        }

        @Test
        void 사용자_발급_제한_초과() {

        }

        @Test
        void 쿠폰_발급_수량_초과() {

        }
    }
}
