package io.hhplus.tdd.domain.product.application;

import io.hhplus.tdd.domain.product.domain.model.Product;
import io.hhplus.tdd.domain.product.domain.service.ProductService;
import io.hhplus.tdd.domain.product.infrastructure.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetProductListUseCase {

    private final ProductRepository productRepository;
    private final ProductService productService;

    /**
     * 상품 리스트 조회 입력 파라미터
     *
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 당 상품 개수 (1~100)
     */
    public record Input(int page, int size) {
        public Input {
            if (page < 0) {
                throw new IllegalArgumentException("페이지 번호는 0 이상이어야 합니다");
            }
            if (size < 1 || size > 100) {
                throw new IllegalArgumentException("페이지 크기는 1~100 사이여야 합니다");
            }
        }
    }

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

    @Transactional(readOnly = true)
    public List<Output> execute(Input input){
        // 페이지에 해당하는 상품 ID만 먼저 조회 
        PageRequest pageRequest = PageRequest.of(input.page(), input.size());
        List<Long> productIds = productRepository.findProductIds(pageRequest);

        // 캐시 우선 조회 + 미스 시 DB 조회 + 캐시 저장
        List<Product> products = productService.getProductsByIds(productIds);

        return products.stream()
                .map(Output::from)
                .toList();
    }


}
