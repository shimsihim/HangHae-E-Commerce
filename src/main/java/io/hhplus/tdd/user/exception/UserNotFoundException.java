package io.hhplus.tdd.user.exception;

import io.hhplus.tdd.common.exception.BusinessException;
import io.hhplus.tdd.common.exception.ErrorCode;

public class UserNotFoundException extends BusinessException {

    private final static ErrorCode errCode = ErrorCode.USER_NOT_FOUND;

    public UserNotFoundException(Long userId) {
        super(errCode , userId);
    }

    @Override
    public ErrorCode getErrCode() {
        return errCode;
    }
}
