package io.hhplus.tdd.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Getter
//@MappedSuperclass
///**
// * 공통 필드 상속용 클래스 => @MappedSuperclass
// * 엔티티들이 공통으로 갖는 컬럼(createdAt, updatedAt) 정의
// * 상속받는 엔티티에 DB 컬럼으로 매핑됨
// */
//@EntityListeners(AuditingEntityListener.class) // JPA 엔티티 이벤트 감지
///**
// * Auditing 적용
// * @CreatedDate → 엔티티가 처음 저장될 때 자동으로 값 채움
// * @LastModifiedDate → 엔티티가 수정될 때 자동으로 값 갱신
// * AuditingEntityListener가 이 이벤트를 감지해서 필드에 값 세팅
// */
@SuperBuilder // 인메모리 db를 위함
@AllArgsConstructor
public abstract class BaseEntity {
//    @CreatedDate
//    @Column(updatable = false , nullable = false)
    private LocalDateTime createdAt;

//    @LastModifiedDate
    private LocalDateTime updatedAt;

//    @PrePersist
//    protected void onCreate() {
//        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
//        if (this.updatedAt == null) this.updatedAt = this.createdAt;
//    }

    public BaseEntity(){
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
        if (this.updatedAt == null) this.updatedAt = this.createdAt;
    }
}