package io.hhplus.tdd.domain.point.application;

import io.hhplus.tdd.domain.point.domain.model.PointHistory;
import io.hhplus.tdd.domain.point.domain.repository.PointHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetPointHistoryListQuery {

    private final PointHistoryRepository pointHistoryRepository;

    public record Input(
            long userId
    ){}

    public record Output(
            long id,
            long userId,
            String type,
            long amount,
            long balanceAfter,
            String description
    ){
        public static Output from(PointHistory pointHistory){
            return new Output(
                    pointHistory.getId(),
                    pointHistory.getUserId(),
                    pointHistory.getType().name(),
                    pointHistory.getAmount(),
                    pointHistory.getBalanceAfter(),
                    pointHistory.getDescription()
            );
        }
    }

//    @Transactional(readOnly = true)
    public List<Output> execute(Input input){
            return pointHistoryRepository.findByUserId(input.userId())
                    .stream()
                    .map(Output::from)
                    .toList();
    }


}
