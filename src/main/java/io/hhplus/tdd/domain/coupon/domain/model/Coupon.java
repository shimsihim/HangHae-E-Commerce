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
}
