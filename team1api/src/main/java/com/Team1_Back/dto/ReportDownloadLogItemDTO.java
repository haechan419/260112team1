package com.Team1_Back.dto;

import java.time.LocalDateTime;

public record ReportDownloadLogItemDTO(
        Long logId,
        Long fileId,
        String fileName,
        Long downloadedBy,
        LocalDateTime downloadedAt
) {}
