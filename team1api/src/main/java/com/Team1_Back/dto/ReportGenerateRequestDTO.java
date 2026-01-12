package com.Team1_Back.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ReportGenerateRequestDTO {

    @NotBlank
    private String reportTypeId;   // 예: PERSONAL_DETAIL_EXCEL, DEPT_SUMMARY_PDF, AI_STRATEGY_PDF


    @NotNull
    @Valid
    private Filters filters;

    @Getter
    @Setter
    public static class Filters {
        private String period;          // "2025-03"
        private String dataScope;       // "MY" / "DEPT" / "ALL" (서버가 role에 맞게 강제 덮어씀)
        private List<String> category;  // ["ALL"] 등
        private String format;          // "PDF" / "EXCEL" (reportType과 일치 검증)
        @JsonAlias({"departmentName"})
        private String department;   // ✅ 프론트 departmentName도 여기로 들어옴
    }
}
