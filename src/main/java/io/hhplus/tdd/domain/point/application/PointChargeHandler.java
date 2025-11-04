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

    public record Output(
            long userId,
            long balance,
            long version
    ){
        public static Output from(UserPoint userPoint){
            return new Output(userPoint.getId(), userPoint.getBalance(), userPoint.getVersion());
        }
    }

//    @Transactional
    public Output handle(Input input){
        UserPoint up = userPointRepository.findByUserId(input.userId()).get();
        up.chargePoint(input.amount());
        UserPoint afterSave = userPointRepository.save(up);
        PointHistory ph = PointHistory.createForCharge(input.userId(), input.amount(), afterSave.getBalance(), input.description());
        pointHistoryRepository.save(ph);
        return Output.from(afterSave);
    }
}
