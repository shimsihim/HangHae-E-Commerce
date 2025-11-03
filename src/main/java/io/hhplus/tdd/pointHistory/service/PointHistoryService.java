package io.hhplus.tdd.pointHistory.service;

import io.hhplus.tdd.pointHistory.dto.response.PointHistoryDTO;

import java.util.List;

public interface PointHistoryService {

    List<PointHistoryDTO> getHistoryById(long userId);
    PointHistoryDTO addUseHistory(long userId, long amount , long afterBalance , String description);
    PointHistoryDTO addChargeHistory(long userId, long amount , long afterBalance , String description);
}
