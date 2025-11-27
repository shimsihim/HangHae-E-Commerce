package io.hhplus.tdd.domain.product.application;

import io.hhplus.tdd.common.cache.CacheNames;
import io.hhplus.tdd.domain.product.domain.model.Product;
import io.hhplus.tdd.domain.product.infrastructure.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetProductListUseCase {

    private final ProductRepository productRepository;

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

    /**
     * 상품 리스트를 페이징하여 조회
     * 첫 번째 페이지(page=0)만 캐싱하여 성능 최적화
     *
     * @param input 페이지 정보
     * @return 상품 리스트
     */
    @Transactional(readOnly = true)
    @Cacheable(
            value = CacheNames.PRODUCT_LIST_ONE_PAGE,
            key = "'page:' + #input.page() + ':size:' + #input.size()",
            condition = "#input.page() <= 3"
    )
    public List<Output> execute(Input input){
        PageRequest pageRequest = PageRequest.of(input.page(), input.size());
        Page<Product> productPage = productRepository.findAll(pageRequest);

        return productPage.getContent()
                .stream()
                .map(Output::from)
                .toList();
    }


}
