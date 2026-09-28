package com.codemonk.common.service;

import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import org.junit.jupiter.api.Test;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BaseEntityTest {

    @Test
    void shouldDefineJpaAndAuditingMetadata() throws NoSuchFieldException {
        assertTrue(BaseEntity.class.isAnnotationPresent(MappedSuperclass.class));

        EntityListeners listeners = BaseEntity.class.getAnnotation(EntityListeners.class);
        assertEquals(AuditingEntityListener.class, listeners.value()[0]);

        assertTrue(BaseEntity.class.getDeclaredField("id").isAnnotationPresent(Id.class));
        assertEquals(
                GenerationType.IDENTITY,
                BaseEntity.class.getDeclaredField("id")
                        .getAnnotation(GeneratedValue.class)
                        .strategy());

        assertTrue(BaseEntity.class.getDeclaredField("createdAt").isAnnotationPresent(CreatedDate.class));
        assertTrue(BaseEntity.class.getDeclaredField("updatedAt").isAnnotationPresent(LastModifiedDate.class));
    }

    @Test
    void shouldStoreBaseEntityValues() {
        BaseEntity entity = new BaseEntity() {
        };
        LocalDateTime createdAt = LocalDateTime.of(2026, 9, 28, 10, 30);
        LocalDateTime updatedAt = createdAt.plusMinutes(5);

        entity.setId(42L);
        entity.setCreatedAt(createdAt);
        entity.setUpdatedAt(updatedAt);

        assertEquals(42L, entity.getId());
        assertEquals(createdAt, entity.getCreatedAt());
        assertEquals(updatedAt, entity.getUpdatedAt());
    }
}
