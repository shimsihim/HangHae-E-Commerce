package io.hhplus.tdd.domain.coupon.application;

import io.hhplus.tdd.domain.coupon.domain.model.Coupon;
import io.hhplus.tdd.domain.coupon.domain.model.UserCoupon;
import io.hhplus.tdd.domain.coupon.infrastructure.repository.CouponRepository;
import io.hhplus.tdd.domain.coupon.infrastructure.repository.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GetUserCouponListUseCase {

    private final UserCouponRepository userCouponRepository;
    private final CouponRepository couponRepository;


    public record Input(
            long userId
    ){}

    public record Output(
            long id,
            long userId,
            long couponId,
            String couponName,
            String discountType,
            int discountValue,
            int duration,
            int minOrderValue,
            String userCouponStatus,
            LocalDate usedAt,
            LocalDate expiredAt
    ){
        public static Output from(UserCoupon userCoupon , Coupon coupon){
            return new Output(
                    userCoupon.getId(),
                    userCoupon.getUserId(),
                    userCoupon.getCouponId(),
                    coupon.getCouponName(),
                    coupon.getDiscountType().toString(),
                    coupon.getDiscountValue(),
                    coupon.getDuration(),
                    coupon.getMinOrderValue(),
                    userCoupon.getStatus().toString(),
                    userCoupon.getUsedAt(),
                    userCoupon.getExpiredAt()
            );
        }
    }

    @Transactional(readOnly = true)
    public List<Output> execute(Input input){
        List<UserCoupon> userCouponList =  userCouponRepository.findByUserId(input.userId())
                .stream()
                .toList();
        return userCouponList.stream().map(userCoupon -> { //인메모리긴 하지만 N+1 발생
            Coupon coupon = couponRepository.findById(userCoupon.getCouponId()).orElseThrow(()->new IllegalArgumentException(""));
            return Output.from(userCoupon , coupon);
        }).toList();
    }


}
