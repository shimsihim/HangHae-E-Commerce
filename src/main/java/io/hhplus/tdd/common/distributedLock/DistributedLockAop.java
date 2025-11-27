package io.hhplus.tdd.common.distributedLock;

import jakarta.persistence.LockTimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.annotation.Order;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
@Order(0)// 트랜젝션보다 먼저 실행되도록 수정
public class DistributedLockAop {
    private final String REDISSON_LOCK_PREFIX  = "LOCK";
    private final RedissonClient redissonClient;

    @Around("@annotation(io.hhplus.tdd.common.distributedLock.DistributedLock)")
    public Object lock(final ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        DistributedLock distributedLock = method.getAnnotation(DistributedLock.class);
        String key = REDISSON_LOCK_PREFIX + getDynamicValue(signature.getParameterNames(), joinPoint.getArgs(), distributedLock.key());
        RLock rLock = redissonClient.getLock(key);

        try {
            // Redisson의 tryLock은 락 획득 성공 시 true, 실패 시 false를 반환합니다.
            boolean available = rLock.tryLock(
                    distributedLock.waitTime(),
                    distributedLock.timeUnit()
            );

            if (!available) {
                // 락 획득에 실패하면 비즈니스 로직을 실행하지 않고 예외를 던집니다.
                log.warn("락 획득 실패: {} (경쟁에서 패배)", key);
                // LockAcquisitionFailureException 은 RuntimeException으로 구현하는 것이 좋습니다.
                throw new LockTimeoutException("락 획득에 실패했습니다.");
            }

            log.info("락 획득 성공 : {}", key);
            return joinPoint.proceed(); // 비즈니스 로직 실행
        } catch (InterruptedException e) {
            log.error("락 획득 중 인터럽트 발생 : {}", key, e);
            // 인터럽트 발생 시 로깅 후 인터럽트 상태를 유지하며 예외를 던짐
            Thread.currentThread().interrupt();
            throw new InterruptedException("락 획득 중 스레드 인터럽트 발생");
        } finally {
            if (rLock.isLocked() && rLock.isHeldByCurrentThread()) { // 락을 소유한 경우에만 해제 시도
                try {
                    log.info("락 반환 : {}", key);
                    rLock.unlock();
                } catch (IllegalMonitorStateException e) {
                    // 이 예외는 Redisson에서 거의 발생하지 않지만, 방어적 코드로는 괜찮습니다.
                    log.warn("Redisson Lock Already Unlocked or not held by current thread: {}", key);
                }
            }
        }
    }


    private static final ExpressionParser parser = new SpelExpressionParser();

    // AOP 파라미터 이름과 값을 받아서 SpEL 표현식을 평가합니다.
    public static Object getDynamicValue(String[] parameterNames, Object[] args, String key) {
        StandardEvaluationContext context = new StandardEvaluationContext();

        // AOP가 넘겨준 파라미터 이름과 실제 값을 Context에 변수로 등록 (예: #input -> Input DTO 객체)
        for (int i = 0; i < parameterNames.length; i++) {
            context.setVariable(parameterNames[i], args[i]);
        }

        // SpEL 키(예: #input.couponId)를 파싱하여 실제 값을 반환
        return parser.parseExpression(key).getValue(context, Object.class);
    }
}
