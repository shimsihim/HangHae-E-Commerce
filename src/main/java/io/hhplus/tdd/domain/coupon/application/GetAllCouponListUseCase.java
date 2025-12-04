package io.hhplus.tdd.domain.coupon.application;

import io.hhplus.tdd.common.cache.RedisKey;
import io.hhplus.tdd.domain.coupon.domain.model.Coupon;
import io.hhplus.tdd.domain.coupon.infrastructure.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    /**
     * 발급 가능한 모든 쿠폰 리스트 조회
     * 쿠폰 정보는 자주 변경되지 않으므로 TTL 20분 캐싱
     *
     * @return 쿠폰 리스트
     */
    @Transactional(readOnly = true)
    @Cacheable(
            value = RedisKey.COUPON_LIST_NAME,
            key = RedisKey.COUPON_LIST_KEY
    )
    public List<Output> execute(){
            return couponRepository.findAll()
                    .stream()
                    .map(Output::from)
                    .toList();
    }


}
