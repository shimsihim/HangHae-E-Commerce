//package io.hhplus.tdd;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import io.hhplus.tdd.pointHistory.service.PointHistoryService;
//import io.hhplus.tdd.user.dto.request.PointChargeDTO;
//import io.hhplus.tdd.pointHistory.dto.request.PointUseDTO;
//import io.hhplus.tdd.user.dto.response.UserPointDTO;
//import io.hhplus.tdd.user.service.UserPointService;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Nested;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.http.MediaType;
//import org.springframework.test.web.servlet.MockMvc;
//
//import java.util.concurrent.CountDownLatch;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//
//import static org.assertj.core.api.Assertions.*;
//import static org.hamcrest.Matchers.containsString;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
//import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
//@SpringBootTest
//@AutoConfigureMockMvc
//class TddApplicationTest {
//
//    @Autowired
//    private MockMvc mvc;
//    @Autowired
//    private UserPointService userPointService;
//    @Autowired
//    private PointHistoryService pointHistoryService;
//    @Autowired
//    private io.hhplus.tdd.database.UserPointTable userPointTable;
//    @Autowired
//    ObjectMapper objectMapper;
//
//    private final static long testUserId = 1L;
//
//    /**
//     * 동시성 테스트를 위한 헬퍼 메서드
//     * @param threadCount 스레드 개수
//     * @param task 각 스레드에서 실행할 작업
//     */
//    private void executeConcurrentTest(int threadCount, Runnable task) throws InterruptedException {
//        CountDownLatch startLatch = new CountDownLatch(1);
//        CountDownLatch endLatch = new CountDownLatch(threadCount);
//        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
//
//        for (int i = 0; i < threadCount; i++) {
//            executorService.submit(() -> {
//                try {
//                    startLatch.await();
//                    task.run();
//                } catch (InterruptedException e) {
//                    Thread.currentThread().interrupt();
//                } catch (Exception e) {
//                    throw new RuntimeException(e);
//                } finally {
//                    endLatch.countDown();
//                }
//            });
//        }
//
//        startLatch.countDown();
//        endLatch.await();
//        executorService.shutdown();
//    }
//
//    @BeforeEach
//    void setUp() {
//        // 인메모리 테이블 클래스로 인하여 매 테스트마다 포인트 초기화 (유저 1)
//        // UserPointTable의 public API를 사용하여 직접 0으로 설정
//        userPointTable.insertOrUpdate(testUserId, 0L);
//    }
//
//    @Nested
//    @DisplayName("유저 포인트 Get")
//    class GetUserPoint{
//
//        @Test
//        void 유저포인트_획득_정상() throws Exception {
//            //given
//            long userId = testUserId;
//            //when
//            mvc.perform(get("/point/{id}" , userId))
//                    //then
//                    .andExpect(status().isOk())
//                    .andExpect(jsonPath("$.id").value(userId))
//                    .andExpect(jsonPath("$.point").value(0))
//                    .andDo(print());
//        }
//
//        @Test
//        void 유저포인트_획득_오류_음수_아이디() throws Exception {
//            //given
//            long userId = -testUserId;
//            //when
//            mvc.perform(get("/point/{id}" , userId))
//                    //then
//                    .andExpect(status().isBadRequest())
//                    .andExpect(jsonPath("$.message").value(containsString("양수")))
//                    .andDo(print());
//        }
//    }
//
//    @Test
//    void 사용자_포인트_히스토리_조회() throws Exception {
//
//        //given
//        long userId = testUserId * 10;
//        long amount = 1000l;
//        int testCnt = 10;
//        PointChargeDTO pt = new PointChargeDTO(1000l);
//        for (int i = 0; i < testCnt; i++) {
//            pointHistoryService.addChargeHistory(userId , amount);
//        }
//
//        //when
//        mvc.perform(get("/point/{id}/histories",userId))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$").isArray())
//                .andExpect(jsonPath("$.length()").value(testCnt))
//                .andDo(print());
//    }
//
//    @Nested
//    @DisplayName("유저 포인트 충전")
//    class ChargePoint{
//
//        @Test
//        void 충전_1회_정상_테스트() throws Exception {
//            //given
//            long userId = testUserId;
//            PointChargeDTO pt = new PointChargeDTO(1000L);
//            //when
//            mvc.perform(patch("/point/{id}/charge" , userId)
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(pt))
//                    )
//                    //then
//                    .andExpect(status().isOk())
//                    .andExpect(jsonPath("$.id").value(userId))
//                    .andExpect(jsonPath("$.point").value(1000))
//                    .andDo(print());
//        }
//
//        @Test
//        void 충전_1회_실패_음수충전_테스트() throws Exception {
//            //given
//            long userId = testUserId;
//            long amount = -10000;
//            PointChargeDTO pt = new PointChargeDTO(amount);
//            //when
//            mvc.perform(patch("/point/{id}/charge",userId)
//                    .contentType(MediaType.APPLICATION_JSON)
//                    .content(objectMapper.writeValueAsString(pt)))
//            //then
//                    .andExpect(status().isBadRequest())
//                    .andExpect(jsonPath("$.message").value(containsString("양수")));
//
//        }
//
//        @Test
//        void 충전_1회_실패_음수아이디_테스트() throws Exception {
//            //given
//            long userId = -1l;
//            PointChargeDTO dto = new PointChargeDTO(1000l);
//
//
//            //when
//            mvc.perform(patch("/point/{id}/charge" , userId)
//                    .contentType(MediaType.APPLICATION_JSON)
//                    .content(objectMapper.writeValueAsString(dto)))
//            //then
//                    .andExpect(status().isBadRequest())
//                    .andExpect(jsonPath("$.message").value(containsString("양수")))
//                    .andDo(print());
//
//        }
//
//        @Test
//        void 충전_다회_동시성_테스트() throws InterruptedException {
//            //given
//            long userId = 1L;
//            PointChargeDTO pt = new PointChargeDTO(1000L);
//            int threadPool = 7;
//
//            //when
//            executeConcurrentTest(threadPool, () -> {
//                try {
//                    mvc.perform(patch("/point/{id}/charge", userId)
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(pt))
//                    );
//                } catch (Exception e) {
//                    throw new RuntimeException(e);
//                }
//            });
//
//            //then
//            UserPointDTO dto = userPointService.getUserPoint(userId);
//            assertThat(dto.point()).isEqualTo(pt.amount() * threadPool);
//        }
//    }
//
//
//    @Nested
//    @DisplayName("유저 포인트 사용")
//    class UsePoint{
//
//        final static long initPoint = 10000l;
//
//        @BeforeEach
//        void chargeInitPoint(){
//            userPointService.addUserPoint(testUserId , initPoint);
//        }
//
//        @Test
//        void 사용_1회_정상_테스트() throws Exception {
//            //given
//            long usePoint = (long)(Math.random() * initPoint);
//            PointUseDTO pu = new PointUseDTO(usePoint);
//            //when
//            mvc.perform(patch("/point/{id}/use" , testUserId)
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(pu))
//                    )
//                    //then
//                    .andExpect(status().isOk())
//                    .andExpect(jsonPath("$.id").value(testUserId))
//                    .andExpect(jsonPath("$.point").value(initPoint - usePoint))
//                    .andDo(print());
//        }
//
//        @Test
//        void 사용_1회_실패_음수사용_테스트() throws Exception {
//            //given
//            long usePoint = -initPoint;
//            PointUseDTO pu = new PointUseDTO(usePoint);
//            //when
//            mvc.perform(patch("/point/{id}/use" , testUserId)
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(pu))
//                    )
//                    //then
//                    .andExpect(status().isBadRequest())
//                    .andExpect(jsonPath("$.message").value(containsString("양수")));
//
//        }
//
//        @Test
//        void 사용_1회_실패_음수아이디_테스트() throws Exception {
//            //given
//            long userId = -testUserId;
//            long usePoint = (long)(Math.random() * initPoint);
//            PointUseDTO pu = new PointUseDTO(usePoint);
//            //when
//            mvc.perform(patch("/point/{id}/use" , userId)
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(pu))
//                    )
//                    //then
//                    .andExpect(status().isBadRequest())
//                    .andExpect(jsonPath("$.message").value(containsString("양수")))
//                    .andDo(print());
//
//        }
//
//        @Test
//        void 사용_다회_동시성_테스트() throws InterruptedException {
//            //given
//            int threadPool = 7;
//            long usePointPerThread = initPoint / threadPool;
//            PointUseDTO pu = new PointUseDTO(usePointPerThread);
//
//            //when
//            executeConcurrentTest(threadPool, () -> {
//                try {
//                    mvc.perform(patch("/point/{id}/use", testUserId)
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(pu))
//                    ).andDo(print());
//                } catch (Exception e) {
//                    throw new RuntimeException(e);
//                }
//            });
//
//            //then
//            UserPointDTO dto = userPointService.getUserPoint(testUserId);
//            assertThat(dto.point()).isEqualTo(initPoint - (initPoint / threadPool) * threadPool);
//        }
//    }
//
//}