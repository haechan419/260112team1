package com.Team1_Back.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ReportGenerateResponseDTO {
    private Long reportId;
    private String status;     // READY
    private String fileName;
}
