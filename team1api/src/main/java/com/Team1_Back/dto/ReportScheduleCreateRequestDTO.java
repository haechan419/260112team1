package com.Team1_Back.dto;

import java.util.List;

public record ReportScheduleCreateRequestDTO(
        String name,
        String reportTypeId,     // "DEPT_SUMMARY_PDF" 같은 코드
        String dataScope,        // "DEPT"
        String outputFormat,     // "PDF"
        String repeatType,       // "DAILY" | "WEEKLY" | "MONTHLY"
        String time,             // "HH:mm"  예: "11:59"
        List<String> daysOfWeek, // WEEKLY: ["MON","WED"] (DAILY/MONTHLY면 null)
        Integer dayOfMonth,      // MONTHLY: 1~28 (WEEKLY/DAILY면 null)
        String requestedBy       // "ADMIN" 등 (선택)
) {}
