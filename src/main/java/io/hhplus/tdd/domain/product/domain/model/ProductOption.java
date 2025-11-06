package io.hhplus.tdd.domain.product.domain.model;

import io.hhplus.tdd.common.exception.ErrorCode;
import io.hhplus.tdd.domain.product.exception.ProductException;
import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductOption {

    @Setter //인메모리이므로 id 값의 증가를 위해서
    private Long id;
    private Long productId;
    private String optionName;
    private Long price;
    private Long quantity;


    // 재고 차감 (검증 포함)
    public void deduct(long quantity){
        if(this.quantity < quantity){
            throw new ProductException(ErrorCode.PRODUCT_NOT_ENOUGH , this.productId , this.id);
        }
        this.quantity -= quantity;
    }


//  재고 차감 (검증 없음)
    public void deductWithoutValidation(long quantity){
        this.quantity -= quantity;
    }

    // 재고 복구
    public void restore(long originalQuantity){
        this.quantity = originalQuantity;
    }
}
