package io.hhplus.tdd.domain.coupon.application;

import io.hhplus.tdd.domain.coupon.domain.model.Coupon;
import io.hhplus.tdd.domain.coupon.domain.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GetAllCouponListUseCase {

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
            LocalDate validFrom,
            LocalDate validUntil
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
    public List<Output> execute(){
            return couponRepository.findAll()
                    .stream()
                    .map(Output::from)
                    .toList();
    }


}
