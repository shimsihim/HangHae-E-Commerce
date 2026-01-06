package io.hhplus.tdd.domain.product.exception;

import io.hhplus.tdd.common.exception.BusinessException;
import io.hhplus.tdd.common.exception.ErrorCode;

public class ProductException extends BusinessException {

    public ProductException(ErrorCode errorCode) {
        super(errorCode);
    }

    public ProductException(ErrorCode errorCode , long productId ) {
        super(errorCode , productId );
    }

    public ProductException(ErrorCode errorCode , String productIds ) {
        super(errorCode , productIds );
    }

    public ProductException(ErrorCode errorCode , long productId  , long productOptionId) {
        super(errorCode , productId  , productOptionId);
    }
}