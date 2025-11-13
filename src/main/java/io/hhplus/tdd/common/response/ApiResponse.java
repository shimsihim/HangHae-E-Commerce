package io.hhplus.tdd.common.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class ApiResponse<T> {

    private boolean isSuccess;
    private T data;
    private String message;
    private String errorCode;

    // 성공 응답
    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.isSuccess = true;
        response.data = data;
        response.message = null;
        response.errorCode = null;
        return response;
    }

    // 실패 응답
    public static <T> ApiResponse<T> error(T data , String message , String errorCode) {
        ApiResponse<T> response = new ApiResponse<>();
        response.isSuccess = false;
        response.data = data;
        response.message = message;
        response.errorCode = errorCode;
        return response;
    }

    // 실패 응답
    public static <T> ApiResponse<T> error( String message , String errorCode) {
        ApiResponse<T> response = new ApiResponse<>();
        response.isSuccess = false;
        response.data = null;
        response.message = message;
        response.errorCode = errorCode;
        return response;
    }
}
