package io.hhplus.tdd.domain.coupon.domain.model;

import io.hhplus.tdd.common.exception.ErrorCode;
import io.hhplus.tdd.domain.coupon.exception.CouponException;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Coupon {
    @Setter // 인메모리 구조로 인한 초기 생성 시 setter
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

    public void increaseIssuedQuantity() {
        this.issuedQuantity++;
    }

    public void decreaseIssuedQuantity() {
        if(this.issuedQuantity > 0) {
            this.issuedQuantity--;
        }
    }

    //할인 계산
    public long calculateDiscountAmount(long orderAmount) {
        if(orderAmount < this.minOrderValue) {
            return 0;
        }

        if(this.discountType == DiscountType.PERCENTAGE) {
            return (orderAmount * this.discountValue) / 100;
        } else {
            return this.discountValue;
        }
    }

    public void validIssue(){
        LocalDateTime now = LocalDateTime.now();
        if(now.isBefore(this.getValidFrom()) || now.isAfter(this.getValidUntil())){
            throw new CouponException(ErrorCode.COUPON_DURATION_ERR , this.getId());
        }
        if(this.getIssuedQuantity() >= this.getTotalQuantity()){
            throw new CouponException(ErrorCode.COUPON_ISSUE_LIMIT , this.getId());
        }
    }
}
