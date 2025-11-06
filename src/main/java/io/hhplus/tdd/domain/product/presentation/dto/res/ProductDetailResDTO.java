package io.hhplus.tdd.domain.product.presentation.dto.res;

import java.util.List;

public record ProductDetailResDTO(
        Long id,
        String name,
        String description,
        Long basePrice,
        List<ProductOptionResDTO> options
){
    public record ProductOptionResDTO(
            Long id,
            String optionName,
            Long price,
            Long quantity
    ){
        public static ProductOptionResDTO of(Long id,
                                             String optionName,
                                             Long price,
                                             Long quantity){
            return new ProductOptionResDTO(id, optionName, price, quantity);
        }
    }
    public static ProductDetailResDTO of(Long id,
                                         String name,
                                         String description,
                                         Long basePrice,
                                         List<ProductOptionResDTO> options){
        return new ProductDetailResDTO(id, name,description,basePrice,options);
    }



}
