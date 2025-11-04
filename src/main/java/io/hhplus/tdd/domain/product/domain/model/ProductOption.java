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


    //재고 차감
    public void deduct(long quantoity){
        if(this.quantity < quantoity){
            throw new ProductException(ErrorCode.PRODUCT_NOT_ENOUGH , this.productId , this.id);
        }
        this.quantity -=  quantoity;
    }
}
