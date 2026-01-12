package com.Team1_Back.dto;

import com.Team1_Back.domain.enums.DataScope;
import com.Team1_Back.domain.enums.OutputFormat;
import com.Team1_Back.domain.enums.PeriodRule;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

//CR용
public record ReportScheduleUpsertRequestDTO(
        @NotBlank String name,
        @NotNull String reportTypeId,
        @NotNull DataScope dataScope,
        @NotNull OutputFormat outputFormat,
        @NotNull PeriodRule periodRule,
        @NotBlank String cronExpr,
        @NotNull Boolean enabled
) {}

