package io.hhplus.tdd.domain.product.application;

import io.hhplus.tdd.domain.product.domain.model.Product;
import io.hhplus.tdd.domain.product.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetProductListUseCase {

    private final ProductRepository productRepository;

    public record Output(
            Long id,
            String name,
            String description,
            Long base_price
    ){
        public static Output from(Product product){
            return new Output(
                    product.getId(),
                    product.getName(),
                    product.getDescription(),
                    product.getBasePrice()
            );
        }
    }

//    @Transactional(readOnly = true)
    public List<Output> execute(){
            return productRepository.findAll()
                    .stream()
                    .map(Output::from)
                    .toList();
    }


}
