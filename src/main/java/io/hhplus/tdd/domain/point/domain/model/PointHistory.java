package io.hhplus.tdd.domain.point.domain.model;

import io.hhplus.tdd.common.baseEntity.CreatedBaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@Entity
public class PointHistory extends CreatedBaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch =  FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)  // FK
    private UserPoint userPoint;

    @Column(name = "user_id", insertable = false, updatable = false) // 단순 조회용
    private Long userId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TransactionType type;

    @Column(nullable = false)
    private Long amount;

    @Column(nullable = false)
    private Long balanceAfter;

    private String description;

    public static PointHistory createForUse(UserPoint userPoint , long amount , long balanceAfter , String description){
        return PointHistory.builder()
                .userPoint(userPoint)
                .amount(amount)
                .balanceAfter(balanceAfter)
                .description(description)
                .type(TransactionType.USE)
                .build();
    }

    public static PointHistory createForCharge(UserPoint userPoint , long amount , long balanceAfter , String description){
        return PointHistory.builder().userPoint(userPoint)
                .amount(amount)
                .balanceAfter(balanceAfter)
                .description(description)
                .type(TransactionType.CHARGE)
                .build();
    }
}
