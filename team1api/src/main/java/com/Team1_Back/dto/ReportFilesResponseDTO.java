package com.Team1_Back.dto;


public record ReportFilesResponseDTO(
        Long reportId,
        java.util.List<ReportFileItemDTO> files
) {}
