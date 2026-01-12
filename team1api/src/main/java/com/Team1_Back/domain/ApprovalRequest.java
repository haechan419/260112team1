package com.Team1_Back.domain;

import jakarta.persistence.*;
import lombok.*;

/**
 * 승인 요청 엔티티
 * 
 * <p>지출 내역 등의 승인 요청 정보를 관리합니다.
 * 요청 유형(EXPENSE, STORE_ORDER 등)과 참조 ID를 통해 실제 승인 대상과 연결됩니다.
 * 
 * @author Team1
 */
@Entity
@Table(name = "approval_request", indexes = {
    @Index(name = "idx_request_type_ref", columnList = "request_type, ref_id"),
    @Index(name = "idx_approver_status", columnList = "approver_id, status_snapshot"),
    @Index(name = "idx_status_updated", columnList = "status_snapshot, updated_at")
})
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"requester", "approver"})
public class ApprovalRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 요청 유형 (EXPENSE: 지출 내역, STORE_ORDER: 구매 주문 등) */
    @Column(name = "request_type", length = 30, nullable = false)
    private String requestType;

    /** 참조 ID (expense.id 또는 store_order.id 등) */
    @Column(name = "ref_id", nullable = false)
    private Long refId;

    /** 요청자 (승인을 요청한 사용자) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    /** 승인자 (승인을 처리하는 관리자) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approver_id")
    private User approver;

    /** 승인 상태 스냅샷 (요청 시점의 상태를 보존) */
    @Enumerated(EnumType.STRING)
    @Column(name = "status_snapshot", length = 30, nullable = false)
    private ApprovalStatus statusSnapshot;

    // ========== 도메인 메서드 ==========

    /**
     * 승인 상태 스냅샷을 동기화합니다.
     * 
     * <p>실제 승인 대상(예: Expense)의 상태가 변경될 때
     * 승인 요청의 상태 스냅샷도 함께 업데이트합니다.
     * 
     * @param status 동기화할 승인 상태
     */
    public void syncStatusSnapshot(ApprovalStatus status) {
        this.statusSnapshot = status;
    }
}

