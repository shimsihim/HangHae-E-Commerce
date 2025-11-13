package io.hhplus.tdd.common.exception;

public class BusinessException extends RuntimeException{

    private final ErrorCode errorCode;

    public ErrorCode getErrCode(){
        return errorCode;
    };

    protected BusinessException(ErrorCode errorCode) {
        super(errorCode.getErrMsg());
        this.errorCode = errorCode;
    }

    protected BusinessException(ErrorCode errorCode , Object ...args) {
        super(String.format(errorCode.getErrMsg() , args));
        this.errorCode = errorCode;
    }
}
