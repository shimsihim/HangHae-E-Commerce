package io.hhplus.tdd.domain.point.application;

import io.hhplus.tdd.common.exception.ErrorCode;
import io.hhplus.tdd.common.exception.UserNotFoundException;
import io.hhplus.tdd.domain.point.domain.model.UserPoint;
import io.hhplus.tdd.domain.point.infrastructure.repository.UserPointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetUserPointQuery {

    private final UserPointRepository userPointRepository;

    public record Input(
            long userId
    ){}

    public record Output(
            long userId,
            long balance
    ){
        public static Output from(UserPoint userPoint){
            return new Output(userPoint.getId(), userPoint.getBalance());
        }
    }

    @Transactional(readOnly = true)
    public Output execute(Input input){
        UserPoint userPoint = userPointRepository.findById(input.userId())
                .orElseThrow(()-> new UserNotFoundException(ErrorCode.USER_NOT_FOUND , input.userId()));
        return Output.from(userPoint);
    }
}
