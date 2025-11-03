package io.hhplus.tdd.pointHistory.controller;


import io.hhplus.tdd.pointHistory.dto.response.PointHistoryDTO;
import io.hhplus.tdd.pointHistory.service.PointHistoryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "포인트 관리 API", description = "사용자 포인트 충전, 사용, 조회 기능을 제공합니다.")
@RestController
@RequestMapping("/point")
@RequiredArgsConstructor
@Validated
public class PointController {

    private static PointHistoryService pointHistoryService;

    public List<PointHistoryDTO> getPointHistoryList(){
        long userId = 1l;
        return pointHistoryService.getHistoryById(userId);
    }

}
