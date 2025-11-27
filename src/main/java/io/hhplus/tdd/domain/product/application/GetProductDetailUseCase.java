package io.hhplus.tdd.domain.product.application;

import io.hhplus.tdd.common.cache.CacheNames;
import io.hhplus.tdd.common.exception.ErrorCode;
import io.hhplus.tdd.domain.product.domain.model.Product;
import io.hhplus.tdd.domain.product.domain.model.ProductOption;
import io.hhplus.tdd.domain.product.infrastructure.repository.ProductOptionRepository;
import io.hhplus.tdd.domain.product.infrastructure.repository.ProductRepository;
import io.hhplus.tdd.domain.product.exception.ProductException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
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

    /**
     * 상품 상세 정보 조회 (상품 정보 + 옵션 리스트 + 재고)
     * 모든 옵션의 재고가 10개 이상일 때만 캐싱
     * 재고 부족 시 캐싱하지 않아 실시간 데이터 제공
     *
     * @param input 상품 ID
     * @return 상품 상세 정보
     */
    @Transactional(readOnly = true)
    @Cacheable(
            value = CacheNames.PRODUCT_DETAIL,
            key = "'id:' + #input.id()",
            condition = "#result != null && @productCacheCondition.shouldCache(#result)"
    )
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
