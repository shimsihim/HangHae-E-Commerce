package io.hhplus.tdd.domain.order.application;

import io.hhplus.tdd.domain.order.domain.repository.OrderItemRepository;
import io.hhplus.tdd.domain.order.domain.repository.OrderRepository;
import io.hhplus.tdd.domain.order.presentation.dto.req.MakeOrderReqDTO;
import io.hhplus.tdd.domain.product.application.GetProductDetailUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MakeOrderUseCase {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;




    public record Input(
            List<ItemInfo> items,
            Long userCouponId,
            Long usePointAmount
    ) {
        public static Input of(List<ItemInfo> items,
                               Long userCouponId,
                               Long usePointAmount){
            return new Input(items, userCouponId, usePointAmount);
        }

        public record ItemInfo(
                Long productId,
                Long productOptionId,
                Long quantity
        ){
            public static ItemInfo of(long productId, long productOptionId, long quantity){
                return new ItemInfo(productId, productOptionId, quantity);
            }
        }
    }

    public void execute(Input input) {
        //1. 사용자 포인트 검증 ,

        //2. 사용자 쿠폰 검증

        // 3. 재고 검증

        // 사용자 쿠폰 차감

        //재고 차감

        //사용자 포인트 차감


    }
}
