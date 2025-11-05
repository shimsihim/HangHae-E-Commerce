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
    private OrderStatus status;
    private Long totalAmount;
    private Long discountAmount;
    private Long usePointAmount;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
}
