package io.hhplus.tdd.domain.point.domain.model;

import io.hhplus.tdd.common.baseEntity.CreatedBaseEntity;
import io.hhplus.tdd.common.baseEntity.UpdatableBaseEntity;
import io.hhplus.tdd.common.exception.ErrorCode;
import io.hhplus.tdd.domain.point.exception.PointRangeException;
import jakarta.persistence.*;
import lombok.*;

@Getter
@AllArgsConstructor
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class UserPoint extends UpdatableBaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long balance;

    @Version
    private Long version;



    private static final long MAX_POINT = 1_000_000_000L;
    private static final long MIN_CHARGE_AMOUNT = 1_000L;
    private static final long MAX_CHARGE_AMOUNT = 500_000L;
    private static final long MIN_USE_AMOUNT = 100L;


    public void chargePoint(long chargePoint){

        if(chargePoint < 0) throw new PointRangeException(ErrorCode.USER_POINT_MUST_POSITIVE , this.id , chargePoint);

        if(chargePoint < MIN_CHARGE_AMOUNT) throw new PointRangeException(ErrorCode.USER_POINT_CHARGE_MIN_AMOUNT , this.id , chargePoint);

        if(chargePoint > MAX_CHARGE_AMOUNT) throw new PointRangeException(ErrorCode.USER_POINT_CHARGE_MAX_AMOUNT , this.id , chargePoint);

        long newPoint;
        try{
            newPoint = Math.addExact(this.balance, chargePoint);
        }
        catch(ArithmeticException e){
            throw new PointRangeException(ErrorCode.USER_POINT_OVERFLOW ,this.id , this.balance , chargePoint);
        }

        if(newPoint > MAX_POINT) throw new PointRangeException(ErrorCode.USER_POINT_MAX_EXCEEDED , this.id , this.balance , chargePoint);
        this.balance = newPoint;
    }

    public void usePoint(long usePoint){
        long newPoint = validUsePoint(usePoint);
        this.balance = newPoint;
    }

    public long validUsePoint(long usePoint){
        if(usePoint < 0) throw new PointRangeException(ErrorCode.USER_POINT_MUST_POSITIVE , this.id , usePoint);

        if(usePoint < MIN_USE_AMOUNT) throw new PointRangeException(ErrorCode.USER_POINT_USE_MIN_AMOUNT , this.id , usePoint);

        long newPoint;
        try{
            newPoint = Math.subtractExact(this.balance, usePoint);
            if(newPoint < 0) throw new PointRangeException(ErrorCode.USER_POINT_NOT_ENOUGH , this.id , this.balance , usePoint);
        }
        catch(ArithmeticException e){
            throw new PointRangeException(ErrorCode.USER_POINT_OVERFLOW ,this.id , this.balance , usePoint );
        }
        return newPoint;
    }
}
