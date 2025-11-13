package io.hhplus.tdd.domain.product.presentation;


import io.hhplus.tdd.domain.product.application.GetPopularProductsUseCase;
import io.hhplus.tdd.domain.product.application.GetProductDetailUseCase;
import io.hhplus.tdd.domain.product.application.GetProductListUseCase;
import io.hhplus.tdd.domain.product.presentation.dto.res.ProductDetailResDTO;
import io.hhplus.tdd.domain.product.presentation.dto.res.ProductResDTO;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "상품 관리 API", description = "상품 조회 , 추가 기능을 제공합니다.")
@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
@Validated
public class ProductController {

    private final GetProductListUseCase getProductListUseCase;
    private final GetProductDetailUseCase getProductDetailUseCase;
    private final GetPopularProductsUseCase getPopularProductsUseCase;

    @GetMapping()
    public List<ProductResDTO> getProductList(){
        List<GetProductListUseCase.Output> outputList = getProductListUseCase.execute();
        return outputList.stream().map(out-> ProductResDTO.of(out.id(),out.name(),out.description(),out.base_price())).toList();
    }

    @GetMapping("/{productId}")
    public ProductDetailResDTO getProductDetail(@PathVariable Long productId){
        GetProductDetailUseCase.Output output = getProductDetailUseCase.execute(new GetProductDetailUseCase.Input(productId));
        List<ProductDetailResDTO.ProductOptionResDTO> options = output.options().stream().map(out -> ProductDetailResDTO.ProductOptionResDTO.of(out.id(),out.optionName(),out.price(),out.quantity())).toList();
        return ProductDetailResDTO.of(output.id(),output.name(),output.description(),output.basePrice(),options);
    }

    @GetMapping("/popular")
    public List<ProductResDTO> getPopularProducts(@RequestParam(defaultValue = "5") int limit){
        List<GetPopularProductsUseCase.Output> outputList = getPopularProductsUseCase.execute();
        return outputList.stream()
                .map(out -> ProductResDTO.of(out.productId(), out.name(), out.description(), out.basePrice()))
                .toList();
    }
}
