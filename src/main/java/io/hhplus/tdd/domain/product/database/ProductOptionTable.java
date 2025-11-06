package io.hhplus.tdd.domain.product.database;


import io.hhplus.tdd.domain.product.domain.model.Product;
import io.hhplus.tdd.domain.product.domain.model.ProductOption;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;


@Component
public class ProductOptionTable {

    private final Map<Long, ProductOption> table = new ConcurrentHashMap<>();



    private AtomicLong cursor = new AtomicLong(0);

    public ProductOption insert(ProductOption productOption) {
        long nextCursor = cursor.addAndGet(1);
        throttle(300L);
        productOption.setId(nextCursor);
        table.put(nextCursor , productOption);
        return productOption;
    }

    public ProductOption selectById(Long id) {
        throttle(200L);
        return table.get(id);
    }

    public List<ProductOption> selectAll() {
        return table.values().stream().toList();
    }

    public ProductOption save(ProductOption productOption) {
        throttle(300L);
        table.put(productOption.getId(), productOption);
        return productOption;
    }

    private void throttle(long millis) {
        try {
            TimeUnit.MILLISECONDS.sleep((long) (Math.random() * millis));
        } catch (InterruptedException ignored) {

        }
    }
}
