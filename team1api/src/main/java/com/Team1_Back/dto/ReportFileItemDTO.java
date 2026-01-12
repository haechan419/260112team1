package com.Team1_Back.dto;

public record ReportFileItemDTO(
        Long fileId,
        String fileName,
        String fileType,
        Long fileSize,
        String checksum,
        String fileUrl,
        java.time.LocalDateTime createdAt
) {}