package io.hhplus.tdd.domain.product.application;

import io.hhplus.tdd.common.cache.CacheNames;
import io.hhplus.tdd.common.exception.ErrorCode;
import io.hhplus.tdd.domain.order.infrastructure.repository.OrderItemRepository;
import io.hhplus.tdd.domain.product.domain.model.Product;
import io.hhplus.tdd.domain.product.infrastructure.repository.ProductOptionRepository;
import io.hhplus.tdd.domain.product.infrastructure.repository.ProductRepository;
import io.hhplus.tdd.domain.product.exception.ProductException;
import io.hhplus.tdd.domain.product.infrastructure.repository.ProductSalesDto;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GetPopularProductsUseCase {

    private final OrderItemRepository orderItemRepository;
    private final ProductOptionRepository productOptionRepository;
    private final ProductRepository productRepository;

    public record Output(
            Long productId,
            String name,
            String description,
            Long basePrice,
            Long totalSalesQuantity
    ) {
        public static Output from(Product product, Long totalSalesQuantity) {
            return new Output(
                    product.getId(),
                    product.getName(),
                    product.getDescription(),
                    product.getBasePrice(),
                    totalSalesQuantity
            );
        }
    }

    /**
     * 인기 상품 목록 조회 (최근 3일 기준 판매량 기준)
     * 전체 리스트를 캐싱하여 빠른 응답 제공
     * 매일 12:00에 캐시 워밍업을 통해 자동 갱신
     *
     * @return 인기 상품 리스트
     */
    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.POPULAR_PRODUCTS_LIST,key = "'all'")
    public List<Output> execute() {
        List<ProductSalesDto> products = productRepository.findPopular(LocalDateTime.now().minusDays(3));
        return products.stream().map(productSalesDto -> Output.from(productSalesDto.product() , productSalesDto.totalSalesQuantity())).collect(Collectors.toList());
    }

    
    // 스케쥴러에서 호출 , 무조건 캐시를 갱신
    @Transactional(readOnly = true)
    @CachePut(value = "popularProducts",key = "'all'")
    public List<Output> refreshCache() {
        List<ProductSalesDto> products = productRepository.findPopular(LocalDateTime.now().minusDays(3));
        return products.stream().map(productSalesDto -> Output.from(productSalesDto.product() , productSalesDto.totalSalesQuantity())).collect(Collectors.toList());
    }
}
