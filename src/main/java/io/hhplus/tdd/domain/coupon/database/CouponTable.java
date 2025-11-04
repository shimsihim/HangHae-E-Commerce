package io.hhplus.tdd.domain.coupon.database;


import io.hhplus.tdd.domain.coupon.domain.model.Coupon;
import io.hhplus.tdd.domain.point.domain.model.PointHistory;
import io.hhplus.tdd.domain.point.domain.model.UserPoint;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 해당 Table 클래스는 변경하지 않고 공개된 API 만을 사용해 데이터를 제어합니다.
 */
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

    private void throttle(long millis) {
        try {
            TimeUnit.MILLISECONDS.sleep((long) (Math.random() * millis));
        } catch (InterruptedException ignored) {

        }
    }
}
