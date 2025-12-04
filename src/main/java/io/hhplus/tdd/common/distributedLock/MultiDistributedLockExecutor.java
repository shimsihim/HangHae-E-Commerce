package io.hhplus.tdd.common.distributedLock;

import io.hhplus.tdd.common.cache.RedisKey;
import jakarta.persistence.LockTimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.RedissonMultiLock;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 여러 리소스에 대한 분산 락을 원자적으로 획득/해제하는 유틸리티 클래스
 * <p>
 * RedissonMultiLock을 사용하여 모든 락을 한번에 획득합니다.
 * 데드락 방지를 위해 락 키를 정렬하여 항상 같은 순서로 락을 획득합니다.
 * <p>
 * RedisKey.LOCK_MULTI를 사용하여 일관된 키 프리픽스(LOCK:)를 적용합니다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MultiDistributedLockExecutor {

    private static final long DEFAULT_WAIT_TIME = 10000L;
    private static final TimeUnit DEFAULT_TIME_UNIT = TimeUnit.MILLISECONDS;

    private final RedissonClient redissonClient;

    /**
     * 여러 락을 원자적으로 획득하고 비즈니스 로직을 실행합니다.
     *
     * @param lockKeys 락을 걸 키 리스트 (예: ["PRODUCT_OPTION:1", "PRODUCT_OPTION:2", "USER_POINT:100"])
     * @param task 실행할 비즈니스 로직
     * @throws LockTimeoutException 락 획득 실패 시
     */
    public void executeWithLocks(List<String> lockKeys, Runnable task) {
        if (lockKeys == null || lockKeys.isEmpty()) {
            log.warn("락 키가 비어있습니다. 락 없이 실행합니다.");
            task.run();
            return;
        }

        // 데드락 방지를 위해 락 키를 정렬
        List<String> sortedKeys = new ArrayList<>(lockKeys);
        Collections.sort(sortedKeys);

        // 중복 제거 (같은 키가 여러 번 있을 경우)
        List<String> uniqueKeys = sortedKeys.stream().distinct().toList();

        // RLock 객체들 생성 (RedisKey 사용)
        List<RLock> locks = new ArrayList<>();
        for (String key : uniqueKeys) {
            String lockKey = RedisKey.LOCK_MULTI.getFullKey(key);
            RLock lock = redissonClient.getLock(lockKey);
            locks.add(lock);
            log.debug("락 생성: {}", lockKey);
        }

        // RedissonMultiLock으로 모든 락을 원자적으로 획득
        RedissonMultiLock multiLock = new RedissonMultiLock(locks.toArray(new RLock[0]));

        try {
            // 모든 락을 동시에 획득 시도
            boolean acquired = multiLock.tryLock(DEFAULT_WAIT_TIME, DEFAULT_TIME_UNIT);

            if (!acquired) {
                log.warn("다중 락 획득 실패: {}", uniqueKeys);
                throw new LockTimeoutException("락 획득에 실패했습니다: " + String.join(", ", uniqueKeys));
            }

            log.info("다중 락 획득 성공: {}", uniqueKeys);

            // 모든 락을 획득했으면 비즈니스 로직 실행
            task.run();

        } catch (InterruptedException e) {
            log.error("락 획득 중 인터럽트 발생: {}", uniqueKeys, e);
            Thread.currentThread().interrupt();
            throw new RuntimeException("락 획득 중 스레드 인터럽트 발생", e);
        } finally {
            try {
                log.info("다중 락 해제: {}", uniqueKeys);
                multiLock.unlock();
            } catch (IllegalMonitorStateException e) {
                log.warn("다중 락 해제 실패 (이미 해제되었거나 현재 스레드가 소유하지 않음): {}", uniqueKeys);
            }
        }
    }
}