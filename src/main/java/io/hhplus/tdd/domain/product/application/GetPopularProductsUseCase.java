package io.hhplus.tdd.domain.product.application;

import io.hhplus.tdd.common.exception.ErrorCode;
import io.hhplus.tdd.domain.order.infrastructure.repository.OrderItemRepository;
import io.hhplus.tdd.domain.product.domain.model.Product;
import io.hhplus.tdd.domain.product.infrastructure.repository.ProductOptionRepository;
import io.hhplus.tdd.domain.product.infrastructure.repository.ProductRepository;
import io.hhplus.tdd.domain.product.exception.ProductException;
import io.hhplus.tdd.domain.product.infrastructure.repository.ProductSalesDto;
import lombok.RequiredArgsConstructor;
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

    @Transactional(readOnly = true)
    public List<Output> execute() {
/*        var orderItems = orderItemRepository.findAll();

        Map<Long, Long> salesByOptionId = orderItems.stream()
                .collect(Collectors.groupingBy(
                        item -> item.getProductOptionId(),
                        Collectors.summingLong(item -> item.getQuantity())
                ));

        Set<Long> productiOptionIds = salesByOptionId.keySet();
        Map<Long, Long> salesByProductId = productiOptionIds.stream().map(optionId->{
                return productOptionRepository.findById(optionId)
                        .orElseThrow(() -> new ProductException(ErrorCode.PRODUCT_NOT_FOUND, optionId));
            }).collect(Collectors.groupingBy(
            item -> item.getProductId(),
                    Collectors.summingLong(item -> salesByOptionId.get(item.getId()))
            ));

        return salesByProductId.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(limit)
                .map(entry -> {
                    Product product = productRepository.findById(entry.getKey())
                            .orElseThrow(() -> new ProductException(ErrorCode.PRODUCT_NOT_FOUND, entry.getKey()));
                    return Output.from(product, entry.getValue());
                })
                .toList();*/

        List<ProductSalesDto> products = productRepository.findPopular(LocalDateTime.now().minusDays(3));
        return products.stream().map(productSalesDto -> Output.from(productSalesDto.product() , productSalesDto.totalSalesQuantity())).collect(Collectors.toList());
    }
}
