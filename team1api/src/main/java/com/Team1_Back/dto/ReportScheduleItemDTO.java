package com.Team1_Back.dto;

import java.time.LocalDateTime;
public record ReportScheduleItemDTO(
        Long id,
        String name,
        String reportTypeId,   // ✅ String
        String dataScope,
        String outputFormat,
        String cronExpr,
        boolean isEnabled,
        LocalDateTime nextRunAt,
        LocalDateTime lastRunAt,
        Long lastJobId,
        int failCount,
        String lastError
) {}
