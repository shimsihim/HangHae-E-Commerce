package io.hhplus.tdd.common.lock;

import io.hhplus.tdd.common.exception.ErrorCode;
import io.hhplus.tdd.common.exception.NoLockKeyException;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class LockManager {
    private final Map<LockKey, ConcurrentHashMap<Long, ReentrantLock>> lockMap = new EnumMap<>(LockKey.class);

    @PostConstruct
    public void init() {
        for (LockKey key : LockKey.values()) {
            lockMap.put(key, new ConcurrentHashMap<>());
        }
    }
    public ReentrantLock getLock(LockKey key , Long id) {
        return Optional.ofNullable(lockMap.get(key))
                .map(conMap -> conMap.computeIfAbsent(id , k-> new ReentrantLock()))
                .orElseThrow(()-> new NoLockKeyException(ErrorCode.LOCK_KEY_NOT_FOUND , key));
    }
}

