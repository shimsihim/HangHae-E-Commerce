package io.hhplus.tdd.common.response;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@RestControllerAdvice
public class ApiResponseAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        // 모든 컨트롤러 응답에 적용
        // 단순히 return true일 경우 String값만 반환하면 에러가 남 =>
        // //https://velog.io/@kylekim2123/SpringBoot-ResponseBodyAdvice%EB%A5%BC-%EC%9D%B4%EC%9A%A9%ED%95%9C-%EA%B3%B5%ED%86%B5-%EC%9D%91%EB%8B%B5-%EC%B2%98%EB%A6%AC%EC%99%80-%EA%B4%80%EB%A0%A8-%ED%8A%B8%EB%9F%AC%EB%B8%94-%EC%8A%88%ED%8C%85
        return MappingJackson2HttpMessageConverter.class.isAssignableFrom(converterType);
        // 이를 사용할 경우 String 반환값의 경우 동일한 패턴을 나타내지 못하므로 이도 불완전하긴 함..
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {


        // void/Void 반환 래핑하지 않음
        if (returnType.getParameterType() == void.class || returnType.getParameterType() == Void.class) {
            return null;
        }

        // 이미 ApiResponse이면 그대로 반환 (에러핸들링에서 잡힌 경우)
        if (body instanceof ApiResponse) {
            return body;
        }

        // DTO나 단일 객체일 경우 ApiResponse로 감싸서 반환
        return ApiResponse.success(body);
    }
}
