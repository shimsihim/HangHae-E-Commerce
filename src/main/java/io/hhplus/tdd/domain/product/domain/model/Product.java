package io.hhplus.tdd.domain.product.domain.model;

import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Setter //인메모리이므로 id 값의 증가를 위해서
    private Long id;
    private String name;
    private String description;
    private Long basePrice;

}
