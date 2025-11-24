package io.hhplus.tdd.common.distributedLock;

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
    public Object lock(final ProceedingJoinPoint joinPoint) throws Throwable{
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        DistributedLock distributedLock = method.getAnnotation(DistributedLock.class);
        String key = REDISSON_LOCK_PREFIX + getDynamicValue(signature.getParameterNames(), joinPoint.getArgs(), distributedLock.key());
        RLock rLock = redissonClient.getLock(key);
        try {
//            boolean available = rLock.tryLock(distributedLock.waitTime(), distributedLock.leaseTime(), distributedLock.timeUnit());
            boolean available = rLock.tryLock(distributedLock.waitTime(), distributedLock.timeUnit());
            if (!available) {
                return false;
            }
            log.debug("락 획득 성공 : {}" , key);
            return joinPoint.proceed();
        }
        catch (InterruptedException e) {
            log.debug("에러 : {}" , key);
            throw new InterruptedException();
        }
        finally {
            try {
                log.debug("락 반환 : {}" , key);
                rLock.unlock();   // (4)
            } catch (IllegalMonitorStateException e) {
                log.info("Redisson Lock Already UnLock {} {}");
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
