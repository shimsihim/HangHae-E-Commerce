package io.hhplus.tdd.domain.cart.presentation;

import io.hhplus.tdd.domain.point.application.GetPointHistoryListQuery;
import io.hhplus.tdd.domain.point.application.PointChargeHandler;
import io.hhplus.tdd.domain.point.application.PointUseHandler;
import io.hhplus.tdd.domain.point.presentation.dto.req.UserPointChargeReqDTO;
import io.hhplus.tdd.domain.point.presentation.dto.req.UserPointUseReqDTO;
import io.hhplus.tdd.domain.point.presentation.dto.res.PointHistoryResDTO;
import io.hhplus.tdd.domain.point.presentation.dto.res.PointResDTO;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "장바구니 관리 API", description = "장바구니 추가 , 수량 변경 , 삭제 기능을 지원합니다.")
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Validated
public class CartController {

    private final GetPointHistoryListQuery getPointHistoryListQuery;
    private final PointChargeHandler pointChargeHandler;
    private final PointUseHandler pointUseHandler;


    @GetMapping("/{userId}")
    public List<PointHistoryResDTO> getCartList(@PathVariable Long userId){
    }

    @PostMapping("/charge")
    public PointResDTO chargeBalance(@RequestBody @Validated UserPointChargeReqDTO chargeReq){
        PointChargeHandler.Output output = pointChargeHandler.handle(new PointChargeHandler.Input(chargeReq.userId(), chargeReq.amount(), chargeReq.description()));
        return PointResDTO.of(output.balance());
    }

    @PostMapping("/use")
    public PointResDTO useBalance(@RequestBody @Validated UserPointUseReqDTO useReq){
        PointUseHandler.Output output = pointUseHandler.handle(new PointUseHandler.Input(useReq.userId(), useReq.amount(), useReq.description()));
        return PointResDTO.of(output.balance());
    }


}
