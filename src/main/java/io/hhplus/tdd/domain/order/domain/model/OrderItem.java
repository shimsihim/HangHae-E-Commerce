package io.hhplus.tdd.domain.order.domain.model;

import io.hhplus.tdd.common.baseEntity.CreatedBaseEntity;
import io.hhplus.tdd.domain.point.domain.model.UserPoint;
import io.hhplus.tdd.domain.product.domain.model.Product;
import io.hhplus.tdd.domain.product.domain.model.ProductOption;
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

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)  // FK
    private Product product;

    @Column(name = "product_id", insertable = false, updatable = false) // 단순 조회용
    private Long productId;

    @ManyToOne
    @JoinColumn(name = "product_option_id", nullable = false)  // FK
    private ProductOption productOption;

    @Column(name = "product_option_id", insertable = false, updatable = false) // 단순 조회용
    private Long productOptionId;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private Long unitPrice;

    @Column(nullable = false)
    private Long subtotal;  // unitPrice * quantity

    public static OrderItem create(Order order , Product product , ProductOption productOption , int quantity , Long unitPrice){
        return OrderItem.builder()
                .order(order)
                .orderId(order.getId())
                .product(product)
                .productId(product.getId())
                .productOption(productOption)
                .productOptionId(productOption.getId())
                .quantity(quantity)
                .unitPrice(unitPrice)
                .subtotal(unitPrice * quantity)
                .build();
    }

}
