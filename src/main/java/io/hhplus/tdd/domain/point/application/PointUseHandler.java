package io.hhplus.tdd.domain.point.application;

import io.hhplus.tdd.common.exception.ErrorCode;
import io.hhplus.tdd.common.exception.UserNotFoundException;
import io.hhplus.tdd.common.lock.LockAnn;
import io.hhplus.tdd.common.lock.LockId;
import io.hhplus.tdd.common.lock.LockKey;
import io.hhplus.tdd.domain.point.domain.model.PointHistory;
import io.hhplus.tdd.domain.point.domain.model.UserPoint;
import io.hhplus.tdd.domain.point.domain.repository.PointHistoryRepository;
import io.hhplus.tdd.domain.point.domain.repository.UserPointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PointUseHandler {

    private final PointHistoryRepository pointHistoryRepository;
    private final UserPointRepository userPointRepository;

    public record Input(
            @LockId long userId,
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
    @LockAnn(lockKey = LockKey.USER)
    public Output handle(Input input){
        UserPoint up = userPointRepository.findByUserId(input.userId()).orElseThrow(()-> new UserNotFoundException(ErrorCode.USER_NOT_FOUND , input.userId()));
        up.usePoint(input.amount());
        UserPoint afterSave = userPointRepository.save(up);
        PointHistory ph = PointHistory.createForUse(input.userId(), input.amount(), afterSave.getBalance(), input.description());
        pointHistoryRepository.save(ph);
        return Output.from(afterSave);
    }
}
