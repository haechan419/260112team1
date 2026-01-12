package com.Team1_Back.dto;

import java.util.List;

public record ReportDownloadLogsResponseDTO(
        Long reportId,
        List<ReportDownloadLogItemDTO> items
) {}
