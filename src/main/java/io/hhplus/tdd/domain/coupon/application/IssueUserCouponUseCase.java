package io.hhplus.tdd.domain.coupon.application;

import io.hhplus.tdd.domain.coupon.infrastructure.queue.CouponIssueProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


@Slf4j
@Service
@RequiredArgsConstructor
public class IssueUserCouponUseCase {

    private final CouponIssueProducer couponIssueProducer;

    public record Input(
            long couponId,
            long userId
    ){}

    /**
     * 쿠폰 발급 요청을 Redis Queue에 추가
     * 실제 발급은 CouponIssueConsumer가 순서대로 처리
     */
    public void execute(Input input){
        couponIssueProducer.produce(input.couponId(), input.userId());
        log.info("쿠폰 발급 요청이 큐에 추가됨. couponId: {}, userId: {}", input.couponId(), input.userId());
    }

}
