package io.hhplus.tdd.domain.user.domain;

import io.hhplus.tdd.common.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;


//@Entity
//@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@SuperBuilder
public class User{

//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    @Column(name = "user_id")
    Long id;

//    @Column(nullable = false , unique = true , length = 50)
    String email;

//    @Column(nullable = false , unique = true , length = 100)
    String password;

//    @Column(nullable = false , unique = true , length = 20)
    String name;

//    @Enumerated(EnumType.STRING)
//    @Column(nullable = false , unique = true , length = 20)
    UserRole role;

//    @Column(nullable = false , unique = true)
    Long balance;

    private static final long MAX_POINT = 1_000_000_000L;
    private static final long MIN_CHARGE_AMOUNT = 1_000L;
    private static final long MIN_USE_AMOUNT = 100L;


    public void chargePoint(long chargePoint){

        if(chargePoint < 0) throw new PointRangeException(ErrorCode.USER_POINT_MUST_POSITIVE , this.id , chargePoint);

        if(chargePoint < MIN_CHARGE_AMOUNT) throw new PointRangeException(ErrorCode.USER_POINT_CHARGE_MIN_AMOUNT , this.id , chargePoint);

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
        this.balance = newPoint;
    }

    public void updateBalance(long newPoint){
        this.balance = newPoint;
    }
}
