package com.Team1_Back.dto;

import java.util.List;

public record ReportFileDownloadLogsResponseDTO(
        Long fileId,
        List<ReportDownloadLogItemDTO> items
) {}
