package io.hhplus.tdd.common;

import io.hhplus.tdd.common.exception.ErrorCode;
import io.hhplus.tdd.common.response.ApiResponse;
import io.hhplus.tdd.user.exception.PointRangeException;
import io.hhplus.tdd.user.exception.UserNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
class ApiControllerAdvice {


    //Request DTO검증
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        List<String> errorMessages = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.toList()); // List로 수집

        // 응답 객체에 List를 담거나, List를 하나의 문자열로 결합하여 전달
        String combinedMessage = String.join(" | ", errorMessages);

        return ApiResponse.error(combinedMessage , ErrorCode.USER_POINT_CHARGE_MIN_AMOUNT.getErrMsg());
    }

//    @ExceptionHandler(ConstraintViolationException.class)
//    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException e) {
//        String detailedMessage = e.getConstraintViolations().stream()
//                .map(violation -> violation.getMessage())
//                .findFirst() // 첫 번째 오류 메시지만 사용
//                .orElse("유효성 검사에 실패했습니다.");
//
//        return ResponseEntity.status(HttpStatus.BAD_REQUEST.value())
//                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.toString(), detailedMessage));
//    }
//
//    @ExceptionHandler(value = UserNotFoundException.class)
//    public ResponseEntity<ErrorResponse> handleUserNotFoundException(UserNotFoundException e) {
//        return ResponseEntity.status(e.getErrCode().getStatus())
//                .body(new ErrorResponse(e.getErrCode().getErrCode(), e.getMessage()));
//    }
//
//    @ExceptionHandler(value = PointRangeException.class)
//    public ResponseEntity<ErrorResponse> handlePointRangeException(PointRangeException e) {
//        return ResponseEntity.status(e.getErrCode().getStatus())
//                .body(new ErrorResponse(e.getErrCode().getErrCode(), e.getMessage()));
//    }
//
//
//    @ExceptionHandler(value = Exception.class)
//    public ResponseEntity<ErrorResponse> handleException(Exception e) {
//        return ResponseEntity.status(500).body(new ErrorResponse("500", "에러가 발생했습니다."));
//    }
}
