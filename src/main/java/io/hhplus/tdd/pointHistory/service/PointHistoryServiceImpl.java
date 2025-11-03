package io.hhplus.tdd.pointHistory.service;

import io.hhplus.tdd.pointHistory.domain.PointHistory;
import io.hhplus.tdd.pointHistory.dto.response.PointHistoryDTO;
import io.hhplus.tdd.pointHistory.repository.PointHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PointHistoryServiceImpl implements PointHistoryService {

    private final PointHistoryRepository pointHistoryRepository;

    @Override
    public List<PointHistoryDTO> getHistoryById(long userId) {
        return pointHistoryRepository.getHistoryByUserId(userId).stream().map(PointHistoryDTO :: from).collect(Collectors.toUnmodifiableList());
    }

    @Override
    public PointHistoryDTO addUseHistory(long userId, long amount , long afterBalance , String description) {
        PointHistory pointHistory = PointHistory.getUsePointHistory(userId , amount , amount , "");
        PointHistory afterSave = pointHistoryRepository.addHistory(pointHistory);
        return PointHistoryDTO.from(afterSave);
    }

    @Override
    public PointHistoryDTO addChargeHistory(long userId, long amount, long afterBalance , String description) {
        PointHistory pointHistory = PointHistory.getChargePointHistory(userId , amount , amount , "");
        PointHistory afterSave = pointHistoryRepository.addHistory(pointHistory);
        return PointHistoryDTO.from(afterSave);
    }
}
