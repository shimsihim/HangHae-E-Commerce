package io.hhplus.tdd.common.aop;


import io.hhplus.tdd.common.exception.ErrorCode;
import io.hhplus.tdd.common.exception.GetLockException;
import io.hhplus.tdd.common.lock.LockAnn;
import io.hhplus.tdd.common.lock.LockId;
import io.hhplus.tdd.common.lock.LockKey;
import io.hhplus.tdd.common.lock.LockManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.RecordComponent;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class LockAspect {
    private final LockManager lockManager;

    @Around(value = "@annotation(lockAnn)")
    public Object around(ProceedingJoinPoint joinPoint, LockAnn lockAnn) throws Throwable {
        log.warn("try lock");
        long waitTime = lockAnn.waitTime();
        TimeUnit timeUnit = lockAnn.timeUnit();
        LockKey lockKey = lockAnn.lockKey();

        // @LockId 어노테이션이 붙은 필드에서 ID 추출

        long id;
        if(lockKey.equals(LockKey.Order)){
            id = LockKey.Order.ordinal();
        }
        else{
            id = extractLockId(joinPoint);
        }

        Lock lock = lockManager.getLock(lockKey, id);
        if(lock.tryLock(waitTime, timeUnit)){
            Long startTime = System.currentTimeMillis();
            try{
                return joinPoint.proceed();
            }
            finally{
                Long endTime = System.currentTimeMillis();
                log.warn("총 소요 시간 : {}" , endTime - startTime);
                lock.unlock();
            }
        }
        else{
            log.warn("락 획득 실패");
            throw new GetLockException(ErrorCode.LOCK_GET_FAIL, id);
        }
    }

    /**
     * 메서드 파라미터에서 @LockId가 붙은 값을 추출합니다.
     * 1. 직접 파라미터에 @LockId가 붙은 경우
     * 2. Record Input 객체의 컴포넌트에 @LockId가 붙은 경우
     */
    private long extractLockId(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Parameter[] parameters = method.getParameters();
        Object[] args = joinPoint.getArgs();

        // 1. 파라미터에 직접 @LockId가 붙은 경우
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].isAnnotationPresent(LockId.class)) {
                Object arg = args[i];
                if (arg instanceof Long) {
                    return (Long) arg;
                } else if (arg instanceof Integer) {
                    return ((Integer) arg).longValue();
                }
                throw new IllegalArgumentException("@LockId는 long 또는 int 타입에만 사용 가능합니다.");
            }
        }

        // 2. Record Input 객체의 컴포넌트에 @LockId가 붙은 경우
        if (args.length > 0 && args[0] != null) {
            Object firstArg = args[0];
            Class<?> argClass = firstArg.getClass();

            // Record인지 확인
            if (argClass.isRecord()) {
                RecordComponent[] components = argClass.getRecordComponents();
                for (RecordComponent component : components) {
                    if (component.isAnnotationPresent(LockId.class)) {
                        try {
                            Object value = component.getAccessor().invoke(firstArg);
                            if (value instanceof Long) {
                                return (Long) value;
                            } else if (value instanceof Integer) {
                                return ((Integer) value).longValue();
                            }
                        } catch (Exception e) {
                            throw new IllegalArgumentException("@LockId 필드 접근 실패: " + component.getName(), e);
                        }
                    }
                }
            }
        }

        throw new IllegalArgumentException("@LockId 어노테이션이 붙은 파라미터를 찾을 수 없습니다.");
    }
}
