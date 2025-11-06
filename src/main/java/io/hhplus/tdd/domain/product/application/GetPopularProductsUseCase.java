package io.hhplus.tdd.domain.product.application;

import io.hhplus.tdd.common.exception.ErrorCode;
import io.hhplus.tdd.domain.order.domain.repository.OrderItemRepository;
import io.hhplus.tdd.domain.product.domain.model.Product;
import io.hhplus.tdd.domain.product.domain.model.ProductOption;
import io.hhplus.tdd.domain.product.domain.repository.ProductOptionRepository;
import io.hhplus.tdd.domain.product.domain.repository.ProductRepository;
import io.hhplus.tdd.domain.product.exception.ProductException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
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

    public List<Output> execute(int limit) {
        var orderItems = orderItemRepository.findAll();

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
                .toList();
    }
}
