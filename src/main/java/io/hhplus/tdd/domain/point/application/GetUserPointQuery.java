package io.hhplus.tdd.domain.point.application;

import io.hhplus.tdd.common.exception.ErrorCode;
import io.hhplus.tdd.common.exception.UserNotFoundException;
import io.hhplus.tdd.domain.point.domain.model.UserPoint;
import io.hhplus.tdd.domain.point.domain.repository.UserPointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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

    public Output execute(Input input){
        UserPoint userPoint = userPointRepository.findByUserId(input.userId())
                .orElseThrow(()-> new UserNotFoundException(ErrorCode.USER_NOT_FOUND , input.userId()));
        return Output.from(userPoint);
    }
}
