package io.hhplus.tdd.domain.product.application;

import io.hhplus.tdd.common.exception.ErrorCode;
import io.hhplus.tdd.domain.product.domain.model.Product;
import io.hhplus.tdd.domain.product.domain.model.ProductOption;
import io.hhplus.tdd.domain.product.infrastructure.repository.ProductOptionRepository;
import io.hhplus.tdd.domain.product.infrastructure.repository.ProductRepository;
import io.hhplus.tdd.domain.product.exception.ProductException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetProductDetailUseCase {

    private final ProductRepository productRepository;
    private final ProductOptionRepository productOptionRepository;

    public record Input(
            Long id
    ){}

    public record OptionOutput(
            Long id,
            String optionName,
            Long price,
            Long quantity
    ){
        public static OptionOutput from(ProductOption productOption){
            return new OptionOutput(
                    productOption.getId(),
                    productOption.getOptionName(),
                    productOption.getPrice(),
                    productOption.getQuantity()
            );
        }
    }

    public record Output(
            Long id,
            String name,
            String description,
            Long basePrice,
            List<OptionOutput> options
    ){
        public static Output from(Product product , List<ProductOption> options){
            List<OptionOutput> optionOutput = options.stream()
                    .map(option -> new OptionOutput(option.getId(),option.getOptionName(),option.getPrice(),option.getQuantity()))
                    .toList();

            return new Output(
                    product.getId(),
                    product.getName(),
                    product.getDescription(),
                    product.getBasePrice(),
                    optionOutput
            );
        }
    }

    @Transactional(readOnly = true)
    public Output execute(Input input){
        Long productId = input.id;

        Product product = productRepository.findWithOptionsById(productId).orElseThrow(
                ()-> new ProductException(ErrorCode.PRODUCT_NOT_FOUND , productId)
        );
        List<ProductOption> optionList = product.getOptions();
        if (optionList.isEmpty()){
            throw new ProductException(ErrorCode.PRODUCT_NOT_FOUND , productId);
        }

        return Output.from(product,optionList);
    }


}
