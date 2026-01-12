package com.Team1_Back.security;

import lombok.Getter;

public record ReportPrincipal(
        Long userId,
        String role,            // USER / ADMIN
        String departmentName   // 예: 마케팅팀
) {}
