package io.hhplus.tdd.domain.product.database;


import io.hhplus.tdd.domain.product.domain.model.Product;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;


@Component
public class ProductTable {

    private final Map<Long, Product> table = new ConcurrentHashMap<>();



    private AtomicLong cursor = new AtomicLong(0);

    public Product insert(Product product) {
        long nextCursor = cursor.addAndGet(1);
        throttle(300L);
        product.setId(nextCursor);
        table.put(nextCursor , product);
        return product;
    }

    public Product selectById(Long id) {
        throttle(200L);
        return table.get(id);
    }

    public List<Product> selectAll() {
        return table.values().stream().toList();
    }

    public Product save(Product product) {
        throttle(300L);
        table.put(product.getId(), product);
        return product;
    }

    private void throttle(long millis) {
        try {
            TimeUnit.MILLISECONDS.sleep((long) (Math.random() * millis));
        } catch (InterruptedException ignored) {

        }
    }
}
