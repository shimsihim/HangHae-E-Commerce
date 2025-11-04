//package io.hhplus.tdd.common.aop;
//
//import io.hhplus.tdd.domain.user.domain.User;
//import io.hhplus.tdd.domain.user.service.UserService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.core.MethodParameter;
//import org.springframework.stereotype.Component;
//import org.springframework.web.bind.support.WebDataBinderFactory;
//import org.springframework.web.context.request.NativeWebRequest;
//import org.springframework.web.method.support.HandlerMethodArgumentResolver;
//import org.springframework.web.method.support.ModelAndViewContainer;
//
//@Component
//@RequiredArgsConstructor
//public class AuthenticationArgumentResolver implements HandlerMethodArgumentResolver {
//
//    private final UserService userService;
//
//    @Override
//    public boolean supportsParameter(MethodParameter parameter) {
//        boolean a = parameter.hasParameterAnnotation(LoginUser.class);
//        boolean b = parameter.getParameterType().equals(User.class);
//        return parameter.hasParameterAnnotation(LoginUser.class) && parameter.getParameterType().equals(User.class);
//    }
//
//    @Override
//    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
//        String userIdStr = webRequest.getHeader("My-User-Id");
//        if (userIdStr == null || userIdStr.isEmpty()) {
//            throw new Exception();
//        }
//        try{
//            long userId = Long.parseLong(userIdStr);
//            User user = userService.getUserByUserId(userId);
//
//            return user;
//        }
//        catch(NumberFormatException e){
//            throw new Exception();
//        }
//    }
//}
