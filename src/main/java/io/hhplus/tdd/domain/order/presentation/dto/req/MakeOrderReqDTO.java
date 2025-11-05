package io.hhplus.tdd.domain.order.presentation.dto.req;


import java.util.List;

public record MakeOrderReqDTO(
        List<ItemInfo> items,
        Long userCouponId,
        Long usePointAmount
) {

    public record ItemInfo(
            Long productId,
            Long productOptionId,
            Long quantity
    ){

    }
}
