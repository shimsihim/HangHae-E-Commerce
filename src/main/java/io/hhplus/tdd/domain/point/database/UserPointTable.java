package io.hhplus.tdd.domain.point.database;

import io.hhplus.tdd.domain.point.domain.model.UserPoint;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 해당 Table 클래스는 변경하지 않고 공개된 API 만을 사용해 데이터를 제어합니다.
 */
@Component
public class UserPointTable {
    private final Map<Long, UserPoint> table = new ConcurrentHashMap<>();

    public UserPoint selectById(Long id) {
        throttle(200L);
        return table.get(id);
    }

    public UserPoint insertOrUpdate(long id, long amount) {
        throttle(300L);
        UserPoint userPoint = UserPoint.builder()
                .id(id)
                .balance(amount)
                .version(1L)
                .build();
        table.put(id, userPoint);
        return userPoint;
    }

    public UserPoint update(UserPoint userPoint) {
        throttle(300L);
        table.put(userPoint.getId(), userPoint);
        return userPoint;
    }

    private void throttle(long millis) {
        try {
            TimeUnit.MILLISECONDS.sleep((long) (Math.random() * millis));
        } catch (InterruptedException ignored) {

        }
    }
}