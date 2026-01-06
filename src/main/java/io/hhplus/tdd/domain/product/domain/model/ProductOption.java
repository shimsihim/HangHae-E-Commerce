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

    @ManyToOne(fetch = FetchType.LAZY)
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


    /**
     * 재고 충분 여부 검증 (차감하지 않음)
     * 주문 생성 시 검증용
     */
    public long validateStock(int requestQuantity){
        if(this.quantity < requestQuantity){
            throw new ProductException(ErrorCode.PRODUCT_NOT_ENOUGH , this.productId , this.id);
        }
        return this.getPrice() * requestQuantity;
    }

    /**
     * 재고 차감 (결제 완료 시)
     */
    public void deduct(int quantity){
        validateStock(quantity);
        this.quantity -= quantity;
    }

    /**
     * 재고 복구 (주문 취소, 결제 실패 시)
     */
    public void restore(int quantity){
        if(quantity < 0){
            throw new IllegalArgumentException("복구 수량은 양수여야 합니다.");
        }
        this.quantity += quantity;
    }
}
