package io.hhplus.tdd.domain.order.presentation;


import io.hhplus.tdd.domain.order.application.CreateOrderUseCase;
import io.hhplus.tdd.domain.order.application.GetOrderListUseCase;
import io.hhplus.tdd.domain.order.application.PayCompleteOrderUseCase;
import io.hhplus.tdd.domain.order.presentation.dto.req.MakeOrderReqDTO;
import io.hhplus.tdd.domain.order.presentation.dto.req.PayOrderReqDTO;
import io.hhplus.tdd.domain.order.presentation.dto.res.OrderResDTO;
import io.swagger.v3.oas.annotations.tags.Tag;
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
    private final CreateOrderUseCase createOrderUseCase;
    private final PayCompleteOrderUseCase payCompleteOrderUseCase;

    @GetMapping("/{userId}") // 주문 내역 조회
    public List<OrderResDTO> getOrderList(@PathVariable Long userId) {
        List<GetOrderListUseCase.OrderInfo> outputList = getOrderListUseCase.execute(new GetOrderListUseCase.Input(userId));

        return outputList.stream().map(OrderResDTO::from).toList();
    }

    @PostMapping() // 주문 요청
    public OrderResDTO makeOrder(@RequestBody MakeOrderReqDTO makeOrderReqDTO) {
        List<CreateOrderUseCase.Input.ProductInfo> items = makeOrderReqDTO.items().stream().map(item -> new CreateOrderUseCase.Input.ProductInfo(item.productId(), item.productOptionId(),item.quantity())).toList();
        CreateOrderUseCase.Input input = CreateOrderUseCase.Input.of(makeOrderReqDTO.userId(), items , makeOrderReqDTO.userCouponId() , makeOrderReqDTO.usePointAmount());
        CreateOrderUseCase.Output output = createOrderUseCase.execute(input);
        return OrderResDTO.from(output);
    }

    @PostMapping("/pay") // 주문 요청
    public void payOrder(@RequestBody PayOrderReqDTO payOrderReqDTO) {
        payCompleteOrderUseCase.execute(new PayCompleteOrderUseCase.Input(payOrderReqDTO.orderId()));
    }
}
