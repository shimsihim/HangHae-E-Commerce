package io.hhplus.tdd.domain.coupon.database;


import io.hhplus.tdd.domain.coupon.domain.model.UserCoupon;
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
public class UserCouponTable {

    private final Map<Long, UserCoupon> table = new ConcurrentHashMap<>();

    private AtomicLong cursor = new AtomicLong(0);

    public UserCoupon save(UserCoupon userCoupon) {
        Long userCouponId = userCoupon.getId();
        if(userCouponId == null){
            long nextCursor = cursor.addAndGet(1);
            throttle(300L);
            userCoupon.setId(nextCursor);
            table.put(nextCursor , userCoupon);
        }
        else{
            table.put(userCouponId , userCoupon);
        }
        return userCoupon;
    }

    public List<UserCoupon> selectAll(){
        return table.values().stream().toList();
    }

    public UserCoupon selectById(Long id) {
        throttle(200L);
        return table.get(id);
    }

    private void throttle(long millis) {
        try {
            TimeUnit.MILLISECONDS.sleep((long) (Math.random() * millis));
        } catch (InterruptedException ignored) {

        }
    }
}
