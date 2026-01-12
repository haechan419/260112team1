package com.Team1_Back.domain;

import jakarta.persistence.*;
import lombok.*;

/**
 * 영수증 검증 엔티티
 * 
 * <p>관리자가 영수증을 검증한 결과를 저장합니다.
 * 지출 내역과 1:1 관계이며, 검증된 가맹점, 금액, 카테고리 등의 정보를 저장합니다.
 * 
 * @author Team1
 */
@Entity
@Table(name = "receipt_verification", indexes = {
    @Index(name = "idx_verified_by_created", columnList = "verified_by, created_at")
})
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"expense", "verifiedBy"})
public class ReceiptVerification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 검증된 지출 내역 (1:1 관계) */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_id", nullable = false, unique = true)
    private Expense expense;

    /** 검증한 관리자 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by", nullable = false)
    private User verifiedBy;

    /** 검증된 가맹점명 */
    @Column(name = "verified_merchant", length = 150)
    private String verifiedMerchant;

    /** 검증된 금액 */
    @Column(name = "verified_amount")
    private Integer verifiedAmount;

    /** 검증된 카테고리 */
    @Column(name = "verified_category", length = 50)
    private String verifiedCategory;

    /** 검증 사유 (승인/거절 이유 등) */
    @Column(name = "reason", length = 255)
    private String reason;
}

