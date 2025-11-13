package io.hhplus.tdd.domain.point.exception;

import io.hhplus.tdd.common.exception.BusinessException;
import io.hhplus.tdd.common.exception.ErrorCode;

public class PointRangeException extends BusinessException {

    public PointRangeException(ErrorCode errorCode) {
        super(errorCode);
    }

    public PointRangeException(ErrorCode errorCode , long userId , long amount) {
        super(errorCode , userId , amount);
    }

    public PointRangeException(ErrorCode errorCode , long userId  , long nowPoint , long amount) {
        super(errorCode , userId  , nowPoint , amount);
    }
}