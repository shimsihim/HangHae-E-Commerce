package io.hhplus.tdd.domain.coupon.domain.model;

import io.hhplus.tdd.common.baseEntity.CreatedBaseEntity;
import io.hhplus.tdd.common.baseEntity.UpdatableBaseEntity;
import io.hhplus.tdd.common.exception.ErrorCode;
import io.hhplus.tdd.domain.coupon.exception.CouponException;
import io.hhplus.tdd.domain.product.domain.model.ProductOption;
import jakarta.persistence.*;
import lombok.*;
import org.apache.catalina.User;
import org.springframework.data.domain.Auditable;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
public class Coupon extends CreatedBaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter
    private Long id;

    @OneToMany(mappedBy = "coupon")
    private List<UserCoupon> userCoupons = new ArrayList<>();

    @Column(nullable = false)
    private String couponName;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private DiscountType discountType;

    @Column(nullable = false)
    private int discountValue;

    @Column(nullable = false)
    private int totalQuantity;

    @Column(nullable = false)
    private int issuedQuantity;

    @Column(nullable = false)
    private int limitPerUser;

    @Column(nullable = false)
    private int duration;

    @Column(nullable = false)
    private int minOrderValue;

    @Column(nullable = false)
    private LocalDate validFrom;

    @Column(nullable = false)
    private LocalDate validUntil;

    @Version
    private Long version;

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
        LocalDate now = LocalDate.now();
        if(now.isBefore(this.getValidFrom()) || now.isAfter(this.getValidUntil())){
            throw new CouponException(ErrorCode.COUPON_DURATION_ERR , this.getId());
        }
        if(this.getIssuedQuantity() >= this.getTotalQuantity()){
            throw new CouponException(ErrorCode.COUPON_ISSUE_LIMIT , this.getId());
        }
    }
}
