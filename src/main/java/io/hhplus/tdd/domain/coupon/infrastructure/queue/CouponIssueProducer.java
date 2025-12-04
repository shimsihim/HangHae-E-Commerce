package io.hhplus.tdd.domain.coupon.infrastructure.queue;

import io.hhplus.tdd.domain.coupon.presentation.dto.req.CouponIssueReqDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponIssueProducer {

    private static final String COUPON_ISSUE_QUEUE_KEY = "coupon:issue:queue";
    private final RedissonClient redissonClient;

    public void produce(Long couponId, Long userId) {
        // Redisson의 블로킹 큐 객체 가져오기
        RBlockingQueue<CouponIssueReqDTO> queue = redissonClient.getBlockingQueue(COUPON_ISSUE_QUEUE_KEY);

        CouponIssueReqDTO request = new CouponIssueReqDTO(userId, couponId);

        // 큐에 추가 (Redis List에 저장됨)
        queue.add(request);

        log.info("쿠폰 발급 요청 큐 추가 완료. couponId: {}, userId: {}", couponId, userId);
    }
}