package io.hhplus.tdd.domain.product.presentation.dto.res;

public record ProductResDTO(
        Long id,
        String name,
        String description,
        Long base_price
    )
    {
        public static ProductResDTO of(long id,
                                       String name,
                                       String description,
                                       Long base_price){
            return new ProductResDTO(id, name, description, base_price);
        }
}
