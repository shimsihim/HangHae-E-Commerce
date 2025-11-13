package io.hhplus.tdd.domain.order.domain.model;

import io.hhplus.tdd.common.baseEntity.CreatedBaseEntity;
import io.hhplus.tdd.domain.point.domain.model.UserPoint;
import jakarta.persistence.*;
import lombok.*;

@Getter
@AllArgsConstructor
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class OrderItem extends CreatedBaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)  // FK
    private Order order;

    @Column(name = "order_id", insertable = false, updatable = false) // 단순 조회용
    private Long orderId;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Long productOptionId;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private Long unitPrice;

    @Column(nullable = false)
    private Long subTotal;

}
