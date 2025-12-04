package io.hhplus.tdd.domain.coupon.application;

import io.hhplus.tdd.domain.coupon.infrastructure.queue.CouponIssueProducer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class IssueUserCouponUseCaseTest {

    @InjectMocks
    IssueUserCouponUseCase issueUserCouponUseCase;

    @Mock
    CouponIssueProducer couponIssueProducer;

    @Test
    void 쿠폰_발급_요청이_큐에_추가된다() {
        // given
        long couponId = 1L;
        long userId = 1L;
        IssueUserCouponUseCase.Input input = new IssueUserCouponUseCase.Input(couponId, userId);

        // when
        issueUserCouponUseCase.execute(input);

        // then
        verify(couponIssueProducer).produce(couponId, userId);
    }
}
