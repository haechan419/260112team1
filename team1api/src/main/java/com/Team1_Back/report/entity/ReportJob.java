package com.Team1_Back.report.entity;

import com.Team1_Back.domain.enums.DataScope;
import com.Team1_Back.domain.enums.OutputFormat;
import com.Team1_Back.domain.enums.ReportStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

@Getter
@Setter
@Entity
@Table(name = "report_job",
        indexes = {
                @Index(name = "idx_report_job_requested_by", columnList = "requested_by"),
                @Index(name = "idx_report_job_status", columnList = "status"),
                @Index(name = "idx_report_job_created_at", columnList = "created_at")
        })
public class ReportJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "requested_by", nullable = false)
    private Long requestedBy;

    @Column(name = "role_snapshot", nullable = false, length = 20)
    private String roleSnapshot;

    @Column(name = "department_snapshot", length = 100)
    private String departmentSnapshot;

    @Column(name = "report_type_id", nullable = false, length = 50)
    private String reportTypeId;

    // 예: "2025-03", "2025-Q1" 등 (프로젝트 규칙으로 고정하면 더 좋음)
    @Column(length = 20)
    private String period;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_scope", nullable = false, length = 20)
    private DataScope dataScope;

    @Column(name = "category_json", columnDefinition = "json")
    private String categoryJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "output_format", nullable = false, length = 10)
    private OutputFormat outputFormat;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportStatus status;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "file_path", length = 500)
    private String filePath;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;


    @Column(name = "period_start")
    private LocalDate periodStart;

    @Column(name = "period_end")
    private LocalDate periodEnd;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }


    @Column(name="approved_total")
    private Long approvedTotal;


    @Column(name="approved_count")
    private Integer approvedCount;


    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public static LocalDate[] toMonthRangeOrNull(String period) {
        if (period == null || period.isBlank()) return null;

        // "YYYY-MM"만 우선 지원 (MVP)
        if (period.matches("^\\d{4}-\\d{2}$")) {
            YearMonth ym = YearMonth.parse(period);
            return new LocalDate[] { ym.atDay(1), ym.atEndOfMonth() };
        }
        return null; // 나중에 "YYYY-MM-DD~YYYY-MM-DD" 같은 확장 가능
    }
}
