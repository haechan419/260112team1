package com.Team1_Back.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 부서별 실적 엔티티
 * AI 챗봇이 부서간 실적을 비교/분석하기 위한 테이블
 */
@Entity
@Table(name = "department_performance", 
    uniqueConstraints = @UniqueConstraint(
        name = "uk_dept_year_month", 
        columnNames = {"department_name", "year", "month"}
    ),
    indexes = {
        @Index(name = "idx_dept_name", columnList = "department_name"),
        @Index(name = "idx_year_month", columnList = "year, month")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepartmentPerformance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "department_name", nullable = false, length = 50)
    private String departmentName;  // 개발1팀, 개발2팀, 영업팀 등

    @Column(nullable = false)
    private Integer year;  // 2025

    @Column(nullable = false)
    private Integer month;  // 1~12

    @Column(name = "sales_amount")
    @Builder.Default
    private Long salesAmount = 0L;  // 매출액 (원)

    @Column(name = "contract_count")
    @Builder.Default
    private Integer contractCount = 0;  // 계약 건수

    @Column(name = "project_count")
    @Builder.Default
    private Integer projectCount = 0;  // 진행 프로젝트 수

    @Column(name = "target_achievement_rate", precision = 5, scale = 2)
    private BigDecimal targetAchievementRate;  // 목표 달성률 (%)

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

