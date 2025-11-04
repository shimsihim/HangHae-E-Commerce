package io.hhplus.tdd.domain.coupon.domain;

import io.hhplus.tdd.common.exception.ErrorCode;
import io.hhplus.tdd.domain.coupon.domain.model.Coupon;
import io.hhplus.tdd.domain.coupon.exception.CouponException;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class CouponValidator {
    public void valid(Coupon coupon){
        LocalDateTime now = LocalDateTime.now();
        if(now.isBefore(coupon.getValidFrom()) || now.isAfter(coupon.getValidUntil())){
            throw new CouponException(ErrorCode.COUPON_DURATION_ERR , coupon.getId());
        }
        if(coupon.getIssuedQuantity() >= coupon.getTotalQuantity()){
            throw new CouponException(ErrorCode.COUPON_ISSUE_LIMIT , coupon.getId());
        }
    }
}


/*
    private Long id;
    private String couponName;
    private DiscountType discountType;
    private int discountValue;
    private int totalQuantity;
    private int issuedQuantity;
    private int limitPerUser;
    private int duration;
    private int minOrderValue;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
 */