package io.hhplus.tdd.domain.coupon.domain.model;

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

    /**
     * 주문 금액에 대한 쿠폰 할인 금액을 계산합니다.
     * @param orderAmount 주문 금액
     * @return 할인 금액
     */
    public long calculateDiscountAmount(long orderAmount) {
        if(orderAmount < this.minOrderValue) {
            return 0;
        }

        if(this.discountType == DiscountType.PERCENTAGE) {
            // 퍼센트 할인: 주문금액 * (할인율 / 100)
            return (orderAmount * this.discountValue) / 100;
        } else {
            // 고정 금액 할인
            return this.discountValue;
        }
    }
}
