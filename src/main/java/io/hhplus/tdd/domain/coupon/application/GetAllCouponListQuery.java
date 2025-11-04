package io.hhplus.tdd.domain.coupon.application;

import io.hhplus.tdd.domain.coupon.domain.model.Coupon;
import io.hhplus.tdd.domain.coupon.domain.model.DiscountType;
import io.hhplus.tdd.domain.coupon.domain.repository.CouponRepository;
import io.hhplus.tdd.domain.point.domain.model.PointHistory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GetAllCouponListQuery {

    private final CouponRepository couponRepository;

    public record Output(
            long id,
            String couponName,
            String discountType,
            int discountValue,
            int totalQuantity,
            int issuedQuantity,
            int limitPerUser,
            int duration,
            int minOrderValue,
            LocalDateTime validFrom,
            LocalDateTime validUntil
    ){
        public static Output from(Coupon coupon){
            return new Output(
                    coupon.getId(),
                    coupon.getCouponName(),
                    coupon.getDiscountType().toString(),
                    coupon.getDiscountValue(),
                    coupon.getTotalQuantity(),
                    coupon.getIssuedQuantity(),
                    coupon.getLimitPerUser(),
                    coupon.getDuration(),
                    coupon.getMinOrderValue(),
                    coupon.getValidFrom(),
                    coupon.getValidUntil()
            );
        }
    }

//    @Transactional(readOnly = true)
    public List<Output> handle(){
            return couponRepository.findAll()
                    .stream()
                    .map(Output::from)
                    .toList();
    }


}
