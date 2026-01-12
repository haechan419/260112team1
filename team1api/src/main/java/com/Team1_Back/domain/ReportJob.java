package com.Team1_Back.domain;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ReportJob {
    private Long id;

    private Long requestedBy;
    private String roleSnapshot;
    private String departmentSnapshot;

    private String reportTypeId;
    private String period;
    private String dataScope;
    private String categoryJson;
    private String outputFormat;

    private String status;
    private String fileName;
    private String filePath;
    private String errorMessage;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
