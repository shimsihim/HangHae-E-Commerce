//package io.hhplus.tdd.pointHistory.service;
//
//import io.hhplus.tdd.pointHistory.domain.PointHistory;
//import io.hhplus.tdd.pointHistory.domain.TransactionType;
//import io.hhplus.tdd.user.dto.response.PointHistoryDTO;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.util.ArrayList;
//import java.util.List;
//
//import static org.assertj.core.api.Assertions.*;
//import static org.mockito.BDDMockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class PointHistoryServiceImplTest {
//    @InjectMocks
//    PointHistoryServiceImpl pointHistoryService;
//
//    @Mock
//    PointHistoryRepository pointHistoryRepository;
//
//    @Test
//    @DisplayName("포인트_충전_사용_히스토리_조회")
//    void 포인트_충전_사용_히스토리_조회(){
//        //given
//        long userId = 1l;
//        List<PointHistory> list = new ArrayList<>();
//        list.add(new PointHistory(0,userId,1000, TransactionType.CHARGE , System.currentTimeMillis()));
//        list.add(new PointHistory(1,userId,5000, TransactionType.CHARGE , System.currentTimeMillis()));
//        list.add(new PointHistory(2,userId,1000, TransactionType.USE , System.currentTimeMillis()));
//        list.add(new PointHistory(3,userId,1000, TransactionType.USE , System.currentTimeMillis()));
//        given(pointHistoryRepository.getHistoryById(userId)).willReturn(list);
//
//        //when
//        List<PointHistoryDTO> dtoList = pointHistoryService.getHistoryById(userId);
//
//        //then
//        assertThat(dtoList).hasSize(list.size());
//        verify(pointHistoryRepository, times(1)).getHistoryById(userId);
//
//        //dto리스트와 list가 완전히 동일한지 검증 필요
//    }
//
//    @Test
//    void 포인트_사용_히스토리_추가(){
//        //given
//        long userId = 1l;
//        long amount = 1000l;
//        PointHistory ph = new PointHistory(0,userId,amount, TransactionType.USE , System.currentTimeMillis());
//        when(pointHistoryRepository.addHistory(any(PointHistory.class))).thenReturn(ph);
//
//        //when
//        PointHistoryDTO dto = pointHistoryService.addUseHistory(userId , amount);
//
//        //then
//        assertThat(dto).usingRecursiveAssertion().isEqualTo(PointHistoryDTO.from(ph));
//    }
//
//    @Test
//    void 충전_히스토리_추가(){
//        //given
//        long userId = 1l;
//        long amount = 1000l;
//        PointHistory ph = new PointHistory(0,userId,amount, TransactionType.CHARGE , System.currentTimeMillis());
//        when(pointHistoryRepository.addHistory(any(PointHistory.class))).thenReturn(ph);
//
//        //when
//        PointHistoryDTO dto = pointHistoryService.addChargeHistory(userId , amount);
//
//        //then
//        assertThat(dto).usingRecursiveAssertion().isEqualTo(PointHistoryDTO.from(ph));
//    }
//}