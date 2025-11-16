package io.hhplus.tdd.domain.order.presentation.dto.req;


import java.util.List;

public record MakeOrderReqDTO(
        Long userId,
        List<ItemInfo> items,
        Long userCouponId,
        Long usePointAmount
) {

    public record ItemInfo(
            Long productId,
            Long productOptionId,
            int quantity
    ){
    }
}
