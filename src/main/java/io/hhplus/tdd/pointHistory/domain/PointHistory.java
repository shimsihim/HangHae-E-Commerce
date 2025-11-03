package io.hhplus.tdd.pointHistory.domain;

import io.hhplus.tdd.common.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

//@Entity
//@Table(name = "point_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@SuperBuilder
public class PointHistory extends BaseEntity {

//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    @Column(name = "point_history_id")
    private long id;

//    @Column(name = "user_id" , nullable = false , unique = true)
    private long userId;

//    @Enumerated(EnumType.STRING)
//    @Column(name = "type", nullable = false, length = 10)
    private TransactionType type;

//    @Column(name = "amount", nullable = false)
    private long amount;

//    @Column(name = "balance_after", nullable = false)
    private long balanceAfter;

//    @Column(name = "description", length = 255)
    private String description;

    public static PointHistory getUsePointHistory(long userId , long amount , long balanceAfter , String description){
        return PointHistory.builder().userId(userId)
                .amount(amount)
                .balanceAfter(balanceAfter)
                .description(description)
                .type(TransactionType.USE)
                .build();
    }

    public static PointHistory getChargePointHistory(long userId , long amount , long balanceAfter , String description){
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
