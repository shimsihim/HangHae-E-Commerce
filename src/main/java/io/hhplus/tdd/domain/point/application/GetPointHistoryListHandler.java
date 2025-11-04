package io.hhplus.tdd.domain.point.application;

import io.hhplus.tdd.domain.point.domain.model.PointHistory;
import io.hhplus.tdd.domain.point.domain.repository.PointHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetPointHistoryListHandler {

    private final PointHistoryRepository pointHistoryRepository;

    public record Input(
            long userId
    ){

    }


    public List<PointHistory> handle(Input input){
            return pointHistoryRepository.findByUserId(input.userId());
    }


}
