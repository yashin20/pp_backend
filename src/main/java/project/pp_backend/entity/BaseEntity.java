package project.pp_backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class) // JPA Auditing 기능을 활성화하는 리스너 추가
@Getter
public abstract class BaseEntity {

    @CreatedDate // 엔티티가 처음 생성될 때 현재 시간을 자동으로 기록합니다.
    @Column(name = "created_at", nullable = false, updatable = false) // DB 컬럼명, null 불가능, 업데이트 불가 설정
    private LocalDateTime createdAt; // LocalDateTime 타입으로 변경

    @LastModifiedDate // 엔티티가 업데이트될 때마다 현재 시간을 자동으로 기록합니다.
    @Column(name = "updated_at", nullable = false) // DB 컬럼명, null 불가능 설정
    private LocalDateTime updatedAt; // LocalDateTime 타입으로 변경
}