package io.hhplus.tdd.domain.coupon.infrastructure.queue;

import io.hhplus.tdd.domain.coupon.domain.service.CouponService;
import io.hhplus.tdd.domain.coupon.presentation.dto.req.CouponIssueReqDTO;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RedissonClient;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponIssueConsumer implements ApplicationRunner {

    private static final String COUPON_ISSUE_QUEUE_KEY = "coupon:issue:queue";
    private final RedissonClient redissonClient;

    // 비즈니스 로직과 트랜잭션을 담당할 외부 서비스
    private final CouponService couponService;

    @Override
    public void run(ApplicationArguments args) {
        new Thread(this::consume).start();
    }

    private void consume() {
        RBlockingQueue<CouponIssueReqDTO> queue = redissonClient.getBlockingQueue(COUPON_ISSUE_QUEUE_KEY);
        while (true) {
            try {
                CouponIssueReqDTO request = queue.take();

                log.info(">> 큐 소비 시작: userId={}", request.userId());

                couponService.issueCoupon(request);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("큐 소비 중 에러 발생", e);
            }
        }
    }
}