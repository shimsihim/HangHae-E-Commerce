package io.hhplus.tdd.common.baseEntity;

import jakarta.persistence.MappedSuperclass;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

@MappedSuperclass
public abstract class UpdatableBaseEntity extends CreatedBaseEntity {
    @LastModifiedDate
    private LocalDateTime updatedAt;
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
