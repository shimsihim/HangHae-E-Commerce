package io.hhplus.tdd.domain.product.database;


import io.hhplus.tdd.domain.product.domain.model.Product;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 해당 Table 클래스는 변경하지 않고 공개된 API 만을 사용해 데이터를 제어합니다.
 */
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
