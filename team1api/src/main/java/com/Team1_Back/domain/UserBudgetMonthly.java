package com.Team1_Back.domain;

import jakarta.persistence.*;
import lombok.*;

/**
 * 사용자별 월간 예산 엔티티
 * 
 * <p>사용자의 월별 지출 예산 한도를 관리합니다.
 * 회계통계 기능에서 예산 집행률 계산 및 예산 초과 인원 조회에 사용됩니다.
 * 
 * <p>⚠️ 주의: 이 엔티티는 회계통계 기능(내 담당)에서 사용됩니다.
 * 
 * @author Team1
 */
@Entity
@Table(name = "user_budget_monthly", indexes = {
    @Index(name = "idx_user_year_month", columnList = "user_id, `year_month`")
})
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"user"})
public class UserBudgetMonthly extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 사용자 (지출 예산을 가진 사용자) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 년월 (YYYY-MM 형식) */
    @Column(name = "`year_month`", length = 7, nullable = false)
    private String yearMonth;

    /** 월간 예산 한도 */
    @Column(name = "monthly_limit", nullable = false)
    private Integer monthlyLimit;

    /** 메모 */
    @Column(name = "note", length = 255)
    private String note;
}

