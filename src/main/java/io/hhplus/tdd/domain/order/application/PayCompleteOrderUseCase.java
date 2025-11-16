package io.hhplus.tdd.domain.order.application;

import io.hhplus.tdd.common.exception.ErrorCode;
import io.hhplus.tdd.domain.coupon.domain.model.Coupon;
import io.hhplus.tdd.domain.coupon.domain.model.UserCoupon;
import io.hhplus.tdd.domain.coupon.domain.service.CouponService;
import io.hhplus.tdd.domain.order.domain.model.Order;
import io.hhplus.tdd.domain.order.domain.model.OrderStatus;
import io.hhplus.tdd.domain.order.exception.OrderException;
import io.hhplus.tdd.domain.order.infrastructure.repository.OrderRepository;
import io.hhplus.tdd.domain.point.domain.model.UserPoint;
import io.hhplus.tdd.domain.point.domain.service.PointService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PayCompleteOrderUseCase {

    private final OrderRepository orderRepository;
    private final PointService pointService;
    private final CouponService couponService;

    public record Input(
            long orderId
    ){
    }

    public void execute(Input input){
        Order order = orderRepository.findById(input.orderId()).orElseThrow(() -> new OrderException(ErrorCode.ORDER_NOT_FOUND, input.orderId()));

        if(order.getStatus() != OrderStatus.PENDING){
            throw new OrderException(ErrorCode.ORDER_NOT_VALID , input.orderId());
        }

        UserCoupon userCoupon = order.getUserCoupon();
        Coupon coupon = userCoupon.getCoupon();
        UserPoint userPoint = order.getUserPoint();
        long usePoint = order.getUsePointAmount();
        long totalAmount = order.getTotalAmount();


        pointService.usePoint(userPoint , usePoint , "Buy Product");
        if(coupon != null && userCoupon != null){
            couponService.useUserCoupon(coupon , userCoupon , totalAmount);
        }
        order.completeOrder();
    }
}
