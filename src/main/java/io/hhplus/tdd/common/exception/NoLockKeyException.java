package io.hhplus.tdd.common.exception;

import io.hhplus.tdd.common.lock.LockKey;

public class NoLockKeyException extends BusinessException{

    public NoLockKeyException(ErrorCode errorCode) {
        super(errorCode);
    }

    public NoLockKeyException(ErrorCode errorCode, LockKey key) {
        super(errorCode, key);
    }
}
