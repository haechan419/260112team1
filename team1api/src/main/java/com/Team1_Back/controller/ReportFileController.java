package com.Team1_Back.controller;

import com.Team1_Back.dto.ReportFileDownloadLogsResponseDTO;
import com.Team1_Back.domain.enums.OutputFormat;
import com.Team1_Back.service.ReportService;
import com.Team1_Back.security.ReportPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/report-files")
public class ReportFileController {

    private final ReportService reportService;

    // 1. 파일 단건 다운로드 (fileId 기준)
    @GetMapping("/{fileId}/download")
    public ResponseEntity<org.springframework.core.io.Resource> downloadByFileId(
            @PathVariable Long fileId,
            @AuthenticationPrincipal ReportPrincipal principal
    ) {
        var r = reportService.downloadFileById(principal, fileId);

        String contentType = (r.format() == OutputFormat.PDF)
                ? "application/pdf"
                : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

        String encoded = java.net.URLEncoder
                .encode(r.fileName(), StandardCharsets.UTF_8)
                .replace("+", "%20");

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + encoded)
                .body(r.resource());
    }

    // 2. 파일 다운로드 로그
    @GetMapping("/{fileId}/downloads")
    public ReportFileDownloadLogsResponseDTO downloadLogsByFile(
            @PathVariable Long fileId,
            @AuthenticationPrincipal ReportPrincipal principal
    ) {
        return reportService.getDownloadLogsByFileId(principal, fileId);
    }
}
