package io.hhplus.tdd.common.exception;

import io.hhplus.tdd.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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
        log.error("request validation error",e);

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
    @ExceptionHandler(value = Exception.class)
    public ApiResponse handleException(Exception e) {
        log.error("error",e);
        return ApiResponse.error("에러가 발생했습니다", "500.");
    }
}
