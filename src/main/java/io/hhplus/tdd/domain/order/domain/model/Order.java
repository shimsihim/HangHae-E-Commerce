package io.hhplus.tdd.domain.order.domain.model;

import io.hhplus.tdd.common.baseEntity.CreatedBaseEntity;
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

    @Column(nullable = false)
    private Long userId;

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
    public static Order createOrder(long userId, Long userCouponId, long totalAmount, long discountAmount, long usePointAmount) {
        long finalAmount = totalAmount - discountAmount - usePointAmount;

        return Order.builder()
                .userId(userId)
                .userCouponId(userCouponId)
                .status(OrderStatus.PENDING)
                .totalAmount(totalAmount)
                .discountAmount(discountAmount)
                .usePointAmount(usePointAmount)
                .finalAmount(finalAmount)
                .build();
    }

    //주문 아이템 생성
    public static OrderItem createOrderItem(Order order,long productId ,  long productOptionId, int quantity, long unitPrice) {
        return OrderItem.builder()
                .order(order)
                .orderId(order.getId())
                .productId(productId)
                .productOptionId(productOptionId)
                .quantity(quantity)
                .unitPrice(unitPrice)
                .subTotal(unitPrice * quantity)
                .build();
    }

    //주문 완료 처리
    public void completeOrder() {
        this.setStatus(OrderStatus.PAID);
        this.setPaidAt(LocalDateTime.now());
    }
}
