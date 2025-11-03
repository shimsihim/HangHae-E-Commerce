package io.hhplus.tdd.common.exception;

public class GetLockException extends BusinessException{
    public GetLockException(ErrorCode errorCode){
        super(errorCode);
    }

    public GetLockException(ErrorCode errorCode, long id){
        super(errorCode , id);
    }
}
