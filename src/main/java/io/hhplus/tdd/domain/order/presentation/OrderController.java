package io.hhplus.tdd.domain.order.presentation;


import io.hhplus.tdd.domain.order.application.GetOrderListUseCase;
import io.hhplus.tdd.domain.order.application.MakeOrderUseCase;
import io.hhplus.tdd.domain.order.presentation.dto.req.GetOrderListReqDTO;
import io.hhplus.tdd.domain.order.presentation.dto.req.MakeOrderReqDTO;
import io.hhplus.tdd.domain.order.presentation.dto.res.OrderResDTO;
import io.hhplus.tdd.domain.product.application.GetProductDetailUseCase;
import io.hhplus.tdd.domain.product.application.GetProductListUseCase;
import io.hhplus.tdd.domain.product.presentation.dto.res.ProductDetailResDTO;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "상품 관리 API", description = "상품 조회 , 추가 기능을 제공합니다.")
@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
@Validated
public class OrderController {

    private final GetOrderListUseCase getOrderListUseCase;
    private final MakeOrderUseCase makeOrderUseCase;

    @GetMapping("/{userId}") // 주문 내역 조회
    public List<OrderResDTO> getOrderList(@PathVariable Long userId) {
        List<GetOrderListUseCase.OrderInfo> outputList = getOrderListUseCase.execute(new GetOrderListUseCase.Input(userId));

        return outputList.stream().map(OrderResDTO::from).toList();
    }

    @PostMapping() // 주문 요청
    public OrderResDTO makeOrder(@RequestBody MakeOrderReqDTO makeOrderReqDTO) {
        List<MakeOrderUseCase.Input.ItemInfo> items = makeOrderReqDTO.items().stream().map(item -> MakeOrderUseCase.Input.ItemInfo.of(item.productId(), item.productOptionId(),item.quantity())).toList();
        MakeOrderUseCase.Input input = MakeOrderUseCase.Input.of(makeOrderReqDTO.userId(), items , makeOrderReqDTO.userCouponId() , makeOrderReqDTO.usePointAmount());
        MakeOrderUseCase.Output output = makeOrderUseCase.execute(input);
        return OrderResDTO.from(output);
    }


}
