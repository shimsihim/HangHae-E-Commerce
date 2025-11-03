package io.hhplus.tdd.common.aop;


import io.hhplus.tdd.common.exception.ErrorCode;
import io.hhplus.tdd.common.exception.GetLockException;
import io.hhplus.tdd.common.lock.LockAnn;
import io.hhplus.tdd.common.lock.LockKey;
import io.hhplus.tdd.common.lock.LockManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class LockAspect {
    private final LockManager lockManager;

    @Around(value = "@annotation(lockAnn) && args(id, ..)")
    public Object around(ProceedingJoinPoint joinPoint , LockAnn lockAnn, long id) throws Throwable {
        log.warn("try lock");
        long waitTime = lockAnn.waitTime();
        TimeUnit timeUnit = lockAnn.timeUnit();
        LockKey lockKey = lockAnn.lockKey();

        Lock lock = lockManager.getLock(lockKey , id);
        if(lock.tryLock(waitTime , timeUnit)){
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
            throw new GetLockException(ErrorCode.LOCK_GET_FAIL , id);
        }


    }
}
