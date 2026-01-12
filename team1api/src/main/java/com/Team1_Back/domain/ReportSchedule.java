package com.Team1_Back.domain;

import com.Team1_Back.domain.enums.DataScope;
import com.Team1_Back.domain.enums.OutputFormat;
import com.Team1_Back.domain.enums.PeriodRule;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "report_schedule", indexes = {
        @Index(name = "idx_schedule_enabled_next", columnList = "is_enabled,next_run_at"),
        @Index(name = "idx_schedule_report_type", columnList = "report_type_id"),
        @Index(name = "idx_schedule_last_job", columnList = "last_job_id")
})
public class ReportSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="name", nullable=false, length=100)
    private String name;

    @Column(name="report_type_id", nullable=false, length=50)
    private String reportTypeId;

    @Enumerated(EnumType.STRING)
    @Column(name="data_scope", nullable=false, length=20)
    private DataScope dataScope;

    @Enumerated(EnumType.STRING)
    @Column(name="output_format", nullable=false, length=20)
    private OutputFormat outputFormat;

    @Column(name="cron_expr", nullable=false, length=100)
    private String cronExpr;

    @Column(name="is_enabled", nullable=false)
    private Boolean isEnabled = true;

    @Column(name="requested_by", nullable=false, length=20)
    private String requestedBy = "SYSTEM"; // ADMIN / SYSTEM

    @Column(name="report_type_code", length=50)
    private String reportTypeCode;

    @Column(name="last_run_at")
    private LocalDateTime lastRunAt;

    @Column(name="next_run_at")
    private LocalDateTime nextRunAt;

    @Column(name="last_job_id")
    private Long lastJobId;

    @Column(name="fail_count", nullable=false)
    private Integer failCount = 0;

    @Lob
    @Column(name="last_error")
    private String lastError;

    @Enumerated(EnumType.STRING)
    @Column(name="period_rule", nullable=false, length=20)
    private PeriodRule periodRule = PeriodRule.CURRENT_MONTH;


    @Column(name="created_at", nullable=false, updatable=false, insertable=false)
    private LocalDateTime createdAt;

    @Column(name="updated_at", nullable=false, insertable=false, updatable=false)
    private LocalDateTime updatedAt;
}
