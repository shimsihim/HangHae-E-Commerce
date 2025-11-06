package io.hhplus.tdd.domain.point.database;


import io.hhplus.tdd.domain.point.domain.model.PointHistory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;


@Component
public class PointHistoryTable {
    private final List<PointHistory> table = new ArrayList<>();
    private AtomicLong cursor = new AtomicLong(1);

    public PointHistory insert(PointHistory ph) {
        long nextCursor = cursor.addAndGet(1);
        throttle(300L);
        ph.setPointHistoryId(nextCursor);
        table.add(ph);
        return ph;
    }

    public List<PointHistory> selectAllByUserId(long userId) {
        return table.stream().filter(pointHistory -> pointHistory.getUserId() == userId).toList();
    }

    private void throttle(long millis) {
        try {
            TimeUnit.MILLISECONDS.sleep((long) (Math.random() * millis));
        } catch (InterruptedException ignored) {

        }
    }
}
