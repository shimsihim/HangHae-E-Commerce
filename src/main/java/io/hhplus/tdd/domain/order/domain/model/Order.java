package io.hhplus.tdd.domain.order.domain.model;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Setter //인메모리이므로 id 값의 증가를 위해서
    private Long id;
    private Long userId;
    private Long userCouponId;
    @Setter
    private OrderStatus status;
    private Long totalAmount;
    private Long discountAmount;
    private Long usePointAmount;
    private Long finalAmount;
    private LocalDateTime createdAt;
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
                .createdAt(LocalDateTime.now())
                .build();
    }

    //주문 아이템 생성
    public static OrderItem createOrderItem(long orderId, long productOptionId, int quantity, long unitPrice) {
        return OrderItem.builder()
                .orderId(orderId)
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
