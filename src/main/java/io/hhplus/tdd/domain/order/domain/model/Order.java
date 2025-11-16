package io.hhplus.tdd.domain.order.domain.model;

import io.hhplus.tdd.common.baseEntity.CreatedBaseEntity;
import io.hhplus.tdd.common.exception.ErrorCode;
import io.hhplus.tdd.domain.coupon.domain.model.UserCoupon;
import io.hhplus.tdd.domain.order.exception.OrderException;
import io.hhplus.tdd.domain.point.domain.model.UserPoint;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "orders")
@ToString(exclude = "userCouponId")
public class Order extends CreatedBaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JoinColumn(name = "user_id", nullable = false)
    @ManyToOne
    private UserPoint userPoint;

    @Column(name = "user_id", insertable = false, updatable = false) // 단순 조회용
    private Long userId;

    @JoinColumn(name = "user_id")
    @ManyToOne
    private UserCoupon userCoupon;

    @Column(name = "user_coupon_id", insertable = false, updatable = false) // 단순 조회용
    private Long userCouponId;

    @Setter
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @Column(nullable = false)
    private Long totalAmount;

    @Column(nullable = false)
    private Long discountAmount;

    @Column(nullable = false)
    private Long usePointAmount;

    @Column(nullable = false)
    private Long finalAmount;

    @Setter
    private LocalDateTime paidAt;


    //주문 생성
    public static Order createOrder(UserPoint userPoint, UserCoupon userCoupon, long totalAmount,
                                    long discountAmount, long usePointAmount) {
        long finalAmount = totalAmount - discountAmount;
        if(finalAmount < 0){
            throw new OrderException(ErrorCode.ORDER_AMOUNT_MUSE_POSITIVE);
        }

        Long userCouponId = (userCoupon != null) ? userCoupon.getId() : null;

        return Order.builder()
                .userId(userPoint.getId())
                .userPoint(userPoint)
                .userCouponId(userCouponId)
                .userCoupon(userCoupon)
                .status(OrderStatus.PENDING)
                .totalAmount(totalAmount)
                .discountAmount(discountAmount)
                .usePointAmount(usePointAmount)
                .finalAmount(finalAmount)
                .build();
    }

    //주문 완료 처리
    public void completeOrder() {
        this.setStatus(OrderStatus.PAID);
        this.setPaidAt(LocalDateTime.now());
    }
}
