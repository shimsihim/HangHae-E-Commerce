package io.hhplus.tdd.domain.order.exception;

import io.hhplus.tdd.common.exception.BusinessException;
import io.hhplus.tdd.common.exception.ErrorCode;

public class OrderException extends BusinessException {

    public OrderException(ErrorCode errorCode) {
        super(errorCode);
    }

    public OrderException(ErrorCode errorCode , long productId ) {
        super(errorCode , productId );
    }

    public OrderException(ErrorCode errorCode , long productId  , long productOptionId) {
        super(errorCode , productId  , productOptionId);
    }
}