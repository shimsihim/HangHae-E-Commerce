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

/**
 * 포인트 충전 롤백 UseCase
 * 결제 실패 시 충전된 포인트를 다시 차감합니다.
 */
@Service
@RequiredArgsConstructor
public class RollbackPointChargeUseCase {

    private final UserPointRepository userPointRepository;
    private final PointHistoryRepository pointHistoryRepository;

    public record Input(
            @LockId long userId,
            long amount,
            String description
    ){}

    public record Output(
            long userId,
            long balance
    ){
        public static Output from(UserPoint userPoint){
            return new Output(userPoint.getId(), userPoint.getBalance());
        }
    }

    @LockAnn(lockKey = LockKey.USER)
    public Output handle(Input input){
        UserPoint up = userPointRepository.findByUserId(input.userId())
                .orElseThrow(()-> new UserNotFoundException(ErrorCode.USER_NOT_FOUND , input.userId()));

        // 충전 롤백 = 사용 처리
        up.usePoint(input.amount());
        UserPoint afterSave = userPointRepository.save(up);

        PointHistory ph = PointHistory.createForUse(input.userId(), input.amount(), afterSave.getBalance(), "ROLLBACK: " + input.description());
        pointHistoryRepository.save(ph);

        return Output.from(afterSave);
    }
}
