package io.hhplus.tdd.domain.coupon.database;


import io.hhplus.tdd.domain.coupon.domain.model.Coupon;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;


@Component
public class CouponTable {

    private final Map<Long, Coupon> table = new ConcurrentHashMap<>();



    private AtomicLong cursor = new AtomicLong(0);

    public Coupon insert(Coupon coupon) {
        long nextCursor = cursor.addAndGet(1);
        throttle(300L);
        coupon.setId(nextCursor);
        table.put(nextCursor , coupon);
        return coupon;
    }

    public Coupon selectById(Long id) {
        throttle(200L);
        return table.get(id);
    }

    public List<Coupon> selectAll() {
        return table.values().stream().toList();
    }

    public Coupon save(Coupon coupon) {
        throttle(300L);
        table.put(coupon.getId(), coupon);
        return coupon;
    }

    private void throttle(long millis) {
        try {
            TimeUnit.MILLISECONDS.sleep((long) (Math.random() * millis));
        } catch (InterruptedException ignored) {

        }
    }
}
