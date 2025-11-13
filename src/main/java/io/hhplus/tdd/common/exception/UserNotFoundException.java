package io.hhplus.tdd.common.exception;

public class UserNotFoundException extends BusinessException {

    public UserNotFoundException(ErrorCode errorCode) {
        super(errorCode);
    }

    public UserNotFoundException(ErrorCode errorCode , long userId) {
        super(errorCode , userId);
    }

}