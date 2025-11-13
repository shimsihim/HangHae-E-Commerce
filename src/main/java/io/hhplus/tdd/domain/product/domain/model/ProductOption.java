package io.hhplus.tdd.domain.product.domain.model;

import io.hhplus.tdd.common.exception.ErrorCode;
import io.hhplus.tdd.domain.product.exception.ProductException;
import jakarta.persistence.*;
import lombok.*;

@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Builder
public class ProductOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter //인메모리이므로 id 값의 증가를 위해서
    private Long id;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)  // FK
    private Product product;

    @Column(name = "product_id", insertable = false, updatable = false) // 단순 조회용
    private Long productId;

    @Column(nullable = false)
    private String optionName;

    @Column(nullable = false)
    private Long price;

    @Column(nullable = false)
    private Long quantity;


    // 재고 차감 검증 O
    public void deduct(long quantity){
        if(this.quantity < quantity){
            throw new ProductException(ErrorCode.PRODUCT_NOT_ENOUGH , this.productId , this.id);
        }
        this.quantity -= quantity;
    }


//  재고 차감 검증 X
    public void deductWithoutValidation(long quantity){
        this.quantity -= quantity;
    }

    // 재고 복구
    public void restore(long originalQuantity){
        this.quantity = originalQuantity;
    }
}
