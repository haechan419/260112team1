package com.Team1_Back.dto;

import com.Team1_Back.domain.enums.DataScope;
import com.Team1_Back.domain.enums.OutputFormat;
import com.Team1_Back.domain.enums.PeriodRule;

import java.time.LocalDateTime;

public record ReportScheduleResponseDTO(
        Long id,
        String name,
        String reportTypeId,   // ✅ String
        DataScope dataScope,
        OutputFormat outputFormat,
        PeriodRule periodRule,
        String cronExpr,
        boolean isEnabled,
        LocalDateTime lastRunAt,
        LocalDateTime nextRunAt,
        Long lastJobId,
        int failCount,
        String lastError
) {}
