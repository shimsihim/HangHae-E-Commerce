package io.hhplus.tdd.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    CMM_BUSINESS_EXCEPTION(HttpStatus.BAD_REQUEST, "C0001" , "비즈니스 에러"),

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U0001" , "유저를 찾을 수 없습니다. 유저 아이디 : %d"),
    USER_POINT_MUST_POSITIVE(HttpStatus.BAD_REQUEST, "U0002" , "포인트 충전 및 사용은 양수만 가능합니다. 유저 아이디 : %d , 충전 금액 : %d"),
    USER_POINT_NOT_ENOUGH(HttpStatus.BAD_REQUEST, "U0003" , "잔액이 부족합니다. 유저 아이디 : %d , 현재 금액 : %d , 사용 금액 : %d"),
    USER_POINT_OVERFLOW(HttpStatus.BAD_REQUEST, "U0004" , "충전 후 금액이 최대값을 초과 합니다. 유저 아이디 : %d ,현재 금액 : %d , 충전(사용) 금액 : %d"),
    USER_POINT_MAX_EXCEEDED(HttpStatus.BAD_REQUEST, "U0005" , "보유 가능한 최대 포인트(1,000,000,000)를 초과합니다. 유저 아이디 : %d , 현재 금액 : %d , 충전 금액 : %d"),
    USER_POINT_CHARGE_MIN_AMOUNT(HttpStatus.BAD_REQUEST, "U0006" , "충전 최소 금액은 1,000원입니다. 유저 아이디 : %d , 충전 금액 : %d"),
    USER_POINT_CHARGE_MAX_AMOUNT(HttpStatus.BAD_REQUEST, "U0007" , "1회 충전 금액은 500,000원을 초과할 수 없습니다. 유저 아이디 : %d , 충전 금액 : %d"),
    USER_POINT_USE_MIN_AMOUNT(HttpStatus.BAD_REQUEST, "U0008" , "사용 최소 금액은 100원입니다. 유저 아이디 : %d , 사용 금액 : %d"),

    LOCK_KEY_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR , "L0001" , "%d 에 해당하는 Lock의 키가 존재하지 않습니다."),
    LOCK_GET_FAIL(HttpStatus.INTERNAL_SERVER_ERROR , "L0002" , "ID : %d 의 락을 얻지 못했습니다."),


    COUPON_NOT_FOUND(HttpStatus.NOT_FOUND, "C0001" , "쿠폰을 찾을 수 없습니다. 쿠폰 아이디 : %d"),
    COUPON_DURATION_ERR(HttpStatus.BAD_REQUEST, "C0002" , "쿠폰의 기한이 유효하지 않습니다. 쿠폰 아이디 : %d"),
    COUPON_ISSUE_LIMIT_PER_USER(HttpStatus.BAD_REQUEST, "C0003" , "유저 당 발급 가능한 쿠폰의 개수를 초과했습니다. 쿠폰 아이디 : %d"),
    COUPON_ISSUE_LIMIT(HttpStatus.BAD_REQUEST, "C0004" , "쿠폰을 더이상 발급할 수 없습니다. 쿠폰 아이디 : %d"),
    COUPON_USER_EXPIRED(HttpStatus.BAD_REQUEST, "C0005" , "사용자 쿠폰이 만료되었습니다. 사용자 쿠폰 아이디 : %d"),
    COUPON_USER_USED(HttpStatus.BAD_REQUEST, "C0006" , "사용자 쿠폰이 만료되었습니다. 사용자 쿠폰 아이디 : %d"),
    COUPON_MIN_ORDER_VALUE_ERR(HttpStatus.BAD_REQUEST, "C0007" , "최소 주문 금액을 충족하지 않습니다. 쿠폰 아이디 : %d");

    private HttpStatus status;
    private String errMsg;
    private String errCode;

    ErrorCode(HttpStatus status , String errCode , String errMsg){
        this.status = status;
        this.errCode = errCode;
        this.errMsg = errMsg;
    }
}
