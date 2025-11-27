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

import java.util.Collections;
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

    /**
     * 전체 상품 리스트 조회 (페이징)
     * 첫 번째 페이지는 캐싱되어 빠른 응답 제공
     *
     * @param page 페이지 번호 (0부터 시작, 기본값: 0)
     * @param size 페이지 당 상품 개수 (기본값: 20)
     * @return 상품 리스트
     */
    @GetMapping()
    public List<ProductResDTO> getProductList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ){
        GetProductListUseCase.Input input = new GetProductListUseCase.Input(page, size);
        List<GetProductListUseCase.Output> outputList = getProductListUseCase.execute(input);
        return outputList.stream()
                .map(out -> ProductResDTO.of(out.id(), out.name(), out.description(), out.base_price()))
                .toList();
    }

    @GetMapping("/{productId}")
    public ProductDetailResDTO getProductDetail(@PathVariable Long productId){
        GetProductDetailUseCase.Output output = getProductDetailUseCase.execute(new GetProductDetailUseCase.Input(productId));
        List<ProductDetailResDTO.ProductOptionResDTO> options = output.options().stream().map(out -> ProductDetailResDTO.ProductOptionResDTO.of(out.id(),out.optionName(),out.price(),out.quantity())).toList();
        return ProductDetailResDTO.of(output.id(),output.name(),output.description(),output.basePrice(),options);
    }

    /**
     * 인기 상품 리스트 조회 (메모리 기반 페이징)
     * 전체 리스트가 캐싱되어 있으므로 메모리 상에서 페이징 처리
     * 매일 12:00에 캐시가 자동 갱신됨
     *
     * @param page 페이지 번호 (0부터 시작, 기본값: 0)
     * @param size 페이지 당 상품 개수 (기본값: 10)
     * @return 인기 상품 리스트
     */
    @GetMapping("/popular")
    public List<ProductResDTO> getPopularProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ){
        // 전체 인기 상품 리스트를 캐시에서 가져옴
        List<GetPopularProductsUseCase.Output> allPopularProducts = getPopularProductsUseCase.execute();

        //  페이징 처리
        int start = page * size;
        int end = Math.min(start + size, allPopularProducts.size());

        // 범위를 벗어나면 빈 리스트 반환
        if (start >= allPopularProducts.size()) {
            return Collections.emptyList();
        }

        // 요청된 페이지의 데이터만 추출하여 반환
        return allPopularProducts.subList(start, end)
                .stream()
                .map(out -> ProductResDTO.of(out.productId(), out.name(), out.description(), out.basePrice()))
                .toList();
    }
}
