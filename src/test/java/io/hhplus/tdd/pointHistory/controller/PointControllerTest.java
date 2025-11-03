//package io.hhplus.tdd.pointHistory.controller;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import io.hhplus.tdd.common.exception.ErrorCode;
//import io.hhplus.tdd.user.controller.UserController;
//import io.hhplus.tdd.user.dto.request.PointChargeDTO;
//import io.hhplus.tdd.user.dto.response.UserPointDTO;
//import io.hhplus.tdd.user.exception.PointRangeException;
//import io.hhplus.tdd.user.service.UserService;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Nested;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
//import org.springframework.boot.test.mock.mockito.MockBean;
//import org.springframework.http.MediaType;
//import org.springframework.test.web.servlet.MockMvc;
//import static org.hamcrest.Matchers.containsString;
//import static org.mockito.BDDMockito.*;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
//import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
//
//@WebMvcTest(UserController.class)
//class PointControllerTest {
//
//    @Autowired
//    MockMvc mockMvc;
//
//    @MockBean
//    UserService userPointService;
//    @MockBean
//    PointHistoryService pointHistoryService;
//
//    @Autowired
//    ObjectMapper objectMapper;
//
//    @Nested
//    @DisplayName("유저 포인트 조회")
//    class SelectUserPoint{
//        @Test
//        void 유저_포인트_가져오기_성공() throws Exception {
//            // given: Mock 객체의 동작을 정의합니다.
//            Long userId = 1L;
//            Long amount = 10000L;
//            UserPointDTO userPointDTO = new UserPointDTO(userId , amount , System.currentTimeMillis());
//            given(userPointService.getUserPoint(userId)).willReturn(userPointDTO);
//
//            // when
//            mockMvc.perform(get("/point/{id}", userId) // GET 요청 수행
//                            .contentType(MediaType.APPLICATION_JSON)) // 요청 헤더 설정
//                    //then
//                    .andExpect(status().isOk()) // HTTP 상태 코드가 200 OK인지 검증
//                    .andExpect(jsonPath("$.id").value(userId)) // 응답 JSON의 id 필드 검증
//                    .andExpect(jsonPath("$.point").value(amount)) // 응답 JSON의 name 필드 검증
//                    .andDo(print()); // 요청 및 응답 전체 내용을 콘솔에 출력 (선택 사항)
//            verify(userPointService).getUserPoint(userId);
//        }
//
//        @Test
//        void 유저_포인트_가져오기_실패_유저아이디_음수() throws Exception {
//            // given: Mock 객체의 동작을 정의합니다.
//            Long userId = -1L;
//            Long amount = 10000L;
//            UserPointDTO userPointDTO = new UserPointDTO(userId , amount , System.currentTimeMillis());
//            given(userPointService.getUserPoint(userId)).willReturn(userPointDTO);
//
//            // when
//            mockMvc.perform(get("/point/{id}", userId) // GET 요청 수행
//                            .contentType(MediaType.APPLICATION_JSON)) // 요청 헤더 설정
//                    //then
//                    .andExpect(status().isBadRequest()) // HTTP 상태 코드가 200 OK인지 검증
//                    .andExpect(jsonPath("$.message").value(containsString("양수")))
//                    .andDo(print()); // 요청 및 응답 전체 내용을 콘솔에 출력 (선택 사항)
//            verify(userPointService , never()).getUserPoint(userId);
//        }
//    }
//
//    @Nested
//    @DisplayName("유저 포인트 충전")
//    class ChargeUserPoint{
//        @Test
//        void 유저_포인트_충전_성공() throws Exception {
//            // given: Mock 객체의 동작을 정의합니다.
//            Long userId = 1L;
//            Long amount = 10000L;
//            UserPointDTO userPointDTO = new UserPointDTO(userId , amount , System.currentTimeMillis());
//            given(userPointService.chargeUserPoint(userId , amount)).willReturn(userPointDTO);
//            PointChargeDTO pointChargeDTO = new PointChargeDTO(amount);
//
//            // when
//            mockMvc.perform(patch("/point/{id}/charge", userId) // GET 요청 수행
//                            .content(objectMapper.writeValueAsString(pointChargeDTO))
//                            .contentType(MediaType.APPLICATION_JSON)) // 요청 헤더 설정
//                    //then
//                    .andExpect(status().isOk())
//                    .andExpect(jsonPath("$.id").value(userId))
//                    .andExpect(jsonPath("$.point").value(amount))
//                    .andDo(print());
//            verify(userPointService).chargeUserPoint(userId , amount);
//
//        }
//
//        @Test
//        void 유저_포인트_충전_실패_유저아이디_음수() throws Exception {
//            // given: Mock 객체의 동작을 정의합니다.
//            Long userId = -1L;
//            Long amount = 10000L;
//            UserPointDTO userPointDTO = new UserPointDTO(userId , amount , System.currentTimeMillis());
//            PointChargeDTO pointChargeDTO = new PointChargeDTO(amount);
//
//            // when
//            mockMvc.perform(patch("/point/{id}/charge", userId) // GET 요청 수행
//                            .content(objectMapper.writeValueAsString(pointChargeDTO))
//                            .contentType(MediaType.APPLICATION_JSON)) // 요청 헤더 설정
//                    //then
//                    .andExpect(status().isBadRequest())
//                    .andExpect(jsonPath("$.message").value(containsString("양수")))
//                    .andDo(print());
//            verify(userPointService,never()).chargeUserPoint(userId , amount);
//        }
//
//        @Test
//        void 유저_포인트_충전_실패_음수충전_도메인단예외() throws Exception {
//            // given: Mock 객체의 동작을 정의합니다.
//            Long userId = 1L;
//            Long amount = 10000L; // 컨트롤러 valid 통과 후 도메인에서 검증 시 예외
//            PointChargeDTO pointChargeDTO = new PointChargeDTO(amount);
//            given(userPointService.chargeUserPoint(userId , amount)).willThrow(new PointRangeException(ErrorCode.USER_POINT_MUST_POSITIVE , userId , amount));
//
//
//            // when
//            mockMvc.perform(patch("/point/{id}/charge", userId) // GET 요청 수행
//                            .content(objectMapper.writeValueAsString(pointChargeDTO))
//                            .contentType(MediaType.APPLICATION_JSON)) // 요청 헤더 설정
//                    //then
//                    .andExpect(status().isBadRequest())
//                    .andExpect(jsonPath("$.message").value(containsString("양수")))
//                    .andExpect(jsonPath("$.code").value("U0002"))
//                    .andDo(print());
//        }
//
//        @Test
//        void 유저_포인트_충전_실패_음수충전_validation예외() throws Exception {
//            // given: Mock 객체의 동작을 정의합니다.
//            Long userId = 1L;
//            Long amount = -10000L; // 컨트롤러 valid 통과 후 도메인에서 검증 시 예외
//            PointChargeDTO pointChargeDTO = new PointChargeDTO(amount);
//
//
//            // when
//            mockMvc.perform(patch("/point/{id}/charge", userId) // GET 요청 수행
//                            .content(objectMapper.writeValueAsString(pointChargeDTO))
//                            .contentType(MediaType.APPLICATION_JSON)) // 요청 헤더 설정
//                    //then
//                    .andExpect(status().isBadRequest())
//                    .andExpect(jsonPath("$.message").value(containsString("양수")))
//                    .andDo(print());
//            verify(userPointService,never()).chargeUserPoint(userId , amount);
//        }
//    }
//
//    //사용도 동일하므로 패스...
//
//
//
//}