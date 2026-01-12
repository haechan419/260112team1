package com.Team1_Back.domain;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 공통 엔티티 베이스 클래스
 * 
 * <p>모든 엔티티가 상속받는 공통 필드(createdAt, updatedAt)를 제공합니다.
 * 
 * <p>⚠️ 주의: 이 클래스는 공통으로 사용됩니다.
 * <ul>
 *   <li>내 코드에서 사용: Expense, ApprovalRequest, ReceiptUpload 등 모든 엔티티가 상속</li>
 *   <li>다른 팀원들도 사용할 수 있습니다</li>
 * </ul>
 * 
 * <p>⚠️ 필드 변경 시 주의:
 * <ul>
 *   <li>필드 추가는 가능하지만, 삭제나 타입 변경 시 모든 엔티티에 영향</li>
 *   <li>특히 {@code createdAt}, {@code updatedAt} 필드는 내 코드에서 사용 중</li>
 * </ul>
 * 
 * @author Team1
 */
@MappedSuperclass
@Getter
public abstract class BaseEntity {

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

