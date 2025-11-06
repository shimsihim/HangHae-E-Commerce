package io.hhplus.tdd.domain.coupon.infrastructure.repository;

import io.hhplus.tdd.domain.coupon.database.CouponTable;
import io.hhplus.tdd.domain.coupon.domain.model.Coupon;
import io.hhplus.tdd.domain.coupon.domain.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Repository
@RequiredArgsConstructor
public class CouponRepositoryImpl implements CouponRepository {

    private final CouponTable couponTable;
    private AtomicLong cursor = new AtomicLong(0);


    @Override
    public List<Coupon> findAll() {
        return couponTable.selectAll();
    }

    @Override
    public Optional<Coupon> findById(long couponId) {
        return Optional.of(couponTable.selectById(couponId));
    }

    @Override
    public Coupon save(Coupon coupon) {
        return couponTable.save(coupon);
    }
}



