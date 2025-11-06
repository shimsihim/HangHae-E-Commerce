package io.hhplus.tdd.domain.order.domain.model;

import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Setter //인메모리이므로 id 값의 증가를 위해서
    private Long id;
    private Long orderItemId;
    private Long orderId;
    private Long productOptionId;
    private int quantity;
    private Long unitPrice;
    private Long subTotal;

}
