package com.Team1_Back.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * 지출 내역 엔티티
 * 
 * <p>사용자의 지출 내역 정보와 승인 상태를 관리합니다.
 * 승인 상태에 따라 수정/삭제 가능 여부가 결정됩니다.
 * 
 * @author Team1
 */
@Entity
@Table(name = "expense", indexes = {
    @Index(name = "idx_user_receipt_date", columnList = "user_id, receipt_date"),
    @Index(name = "idx_approval_status_updated", columnList = "approval_status, updated_at")
})
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"writer"})
public class Expense extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 작성자 (지출 내역을 등록한 사용자) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User writer;

    /** 승인 상태 (DRAFT, SUBMITTED, APPROVED, REJECTED, REQUEST_MORE_INFO) */
    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", length = 30, nullable = false)
    private ApprovalStatus status;

    @Column(name = "merchant", length = 150)
    private String merchant;

    @Column(name = "amount", nullable = false)
    private Integer amount;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "receipt_date", nullable = false)
    private LocalDate receiptDate;

    @Column(name = "receipt_image_url", length = 255)
    private String receiptImageUrl;

    @Column(name = "description", length = 255)
    private String description;

    // ========== 도메인 메서드 ==========

    /**
     * 지출 내역을 승인 요청 상태로 제출합니다.
     * 
     * <p>DRAFT 상태의 지출 내역만 제출할 수 있습니다.
     * 제출 후 상태는 SUBMITTED로 변경됩니다.
     * 
     * @throws IllegalStateException DRAFT 상태가 아닌 경우
     */
    public void submit() {
        if (this.status != ApprovalStatus.DRAFT) {
            throw new IllegalStateException("DRAFT 상태의 지출 내역만 제출할 수 있습니다.");
        }
        this.status = ApprovalStatus.SUBMITTED;
    }

    /**
     * 지출 내역을 승인합니다.
     * 
     * <p>상태를 APPROVED로 변경합니다.
     * 관리자만 호출 가능합니다.
     */
    public void approve() {
        this.status = ApprovalStatus.APPROVED;
    }

    /**
     * 지출 내역을 거절합니다.
     * 
     * <p>상태를 REJECTED로 변경합니다.
     * 관리자만 호출 가능합니다.
     * 
     * @param adminNote 관리자 메모 (현재는 미사용)
     */
    public void reject(String adminNote) {
        this.status = ApprovalStatus.REJECTED;
        // TODO: adminNote는 별도 필드가 없으므로 description에 저장하거나 별도 처리 필요
    }

    /**
     * 추가 정보 요청 상태로 변경합니다.
     * 
     * <p>상태를 REQUEST_MORE_INFO로 변경합니다.
     * 관리자만 호출 가능합니다.
     * 
     * @param adminNote 관리자 메모 (현재는 미사용)
     */
    public void requestMoreInfo(String adminNote) {
        this.status = ApprovalStatus.REQUEST_MORE_INFO;
        // TODO: adminNote는 별도 필드가 없으므로 description에 저장하거나 별도 처리 필요
    }

    /**
     * 초안 상태인지 확인합니다.
     * 
     * @return DRAFT 상태이면 true, 아니면 false
     */
    public boolean isDraft() {
        return this.status == ApprovalStatus.DRAFT;
    }

    /**
     * 수정 가능한 상태인지 확인합니다.
     * 
     * <p>현재는 DRAFT 상태일 때만 수정 가능합니다.
     * 
     * @return 수정 가능하면 true, 아니면 false
     */
    public boolean canModify() {
        return isDraft();
    }

    /**
     * 삭제 가능한 상태인지 확인합니다.
     * 
     * <p>현재는 DRAFT 상태일 때만 삭제 가능합니다.
     * 
     * @return 삭제 가능하면 true, 아니면 false
     */
    public boolean canDelete() {
        return isDraft();
    }
}

