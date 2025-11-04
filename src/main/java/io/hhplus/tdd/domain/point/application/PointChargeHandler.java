package io.hhplus.tdd.domain.point.application;

import io.hhplus.tdd.domain.point.domain.model.PointHistory;
import io.hhplus.tdd.domain.point.domain.model.UserPoint;
import io.hhplus.tdd.domain.point.domain.repository.PointHistoryRepository;
import io.hhplus.tdd.domain.point.domain.repository.UserPointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PointChargeHandler {

    private final PointHistoryRepository pointHistoryRepository;
    private final UserPointRepository userPointRepository;

    public record Input(
            long userId,
            long amount,
            String description
    ){}

    public UserPoint handle(Input input){
        UserPoint up = userPointRepository.findByUserId(input.userId());
        up.chargePoint(input.amount());
        UserPoint afterSave = userPointRepository.save(up);
        PointHistory ph = PointHistory.getChargePointHistory(input.userId,input.amount() , afterSave.getBalance(),input.description());
        pointHistoryRepository.save(ph);
        return afterSave;
    }
}
