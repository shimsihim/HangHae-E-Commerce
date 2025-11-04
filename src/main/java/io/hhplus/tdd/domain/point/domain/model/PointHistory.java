package io.hhplus.tdd.domain.point.domain.model;

import io.hhplus.tdd.common.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PointHistory {

    private Long id;
    private Long userId;
    private TransactionType type;
    private Long amount;
    private Long balanceAfter;
    private String description;

    public static PointHistory createForUse(long userId , long amount , long balanceAfter , String description){
        return PointHistory.builder().userId(userId)
                .amount(amount)
                .balanceAfter(balanceAfter)
                .description(description)
                .type(TransactionType.USE)
                .build();
    }

    public static PointHistory createForCharge(long userId , long amount , long balanceAfter , String description){
        return PointHistory.builder().userId(userId)
                .amount(amount)
                .balanceAfter(balanceAfter)
                .description(description)
                .type(TransactionType.CHARGE)
                .build();
    }

    public void setPointHistoryId(long historyId){
        this.id = historyId;
    }
}
