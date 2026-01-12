package com.Team1_Back.controller;

import com.Team1_Back.dto.*;
import com.Team1_Back.security.ReportPrincipal;
import com.Team1_Back.domain.enums.OutputFormat;
import com.Team1_Back.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    private ReportPrincipal toReportPrincipal(UserDTO user) {
        if (user == null) return null;

        boolean isAdmin = user.getRoleNames() != null &&
                user.getRoleNames().stream().anyMatch(r ->
                        "ADMIN".equalsIgnoreCase(r) || "ROLE_ADMIN".equalsIgnoreCase(r)
                );

        String role = isAdmin ? "ADMIN" : "USER";

        return new ReportPrincipal(user.getId(), role, user.getDepartmentName());
    }



    // 1. 리포트 생성
    @PostMapping("/generate")
    public ResponseEntity<ReportGenerateResponseDTO> generate(
            @AuthenticationPrincipal UserDTO principal,   // ✅ 변경
            @RequestBody @Valid ReportGenerateRequestDTO req
    ) {
        // ✅ principal null 방어(로그인 세션 없을 때)
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        ReportPrincipal rp = toReportPrincipal(principal);
        var r = reportService.generate(rp, req);
        String dept = req.getFilters().getDepartment();

        return ResponseEntity.ok(
                new ReportGenerateResponseDTO(r.reportId(), r.status(), r.fileName())
        );
    }

    // 2. 리포트 기준 최신 파일 다운로드
    @GetMapping("/{reportId}/download")
    public ResponseEntity<Resource> download(
            @AuthenticationPrincipal UserDTO principal,   //  변경
            @PathVariable Long reportId
    ) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        ReportPrincipal rp = toReportPrincipal(principal);
        var d = reportService.download(rp, reportId);

        String contentType = (d.format() == OutputFormat.PDF)
                ? MediaType.APPLICATION_PDF_VALUE
                : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

        String encoded = java.net.URLEncoder
                .encode(d.fileName(), StandardCharsets.UTF_8)
                .replace("+", "%20");

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + encoded)
                .body(d.resource());
    }

    // 3. 리포트에 딸린 파일 목록
    @GetMapping("/{reportId}/files")
    public ResponseEntity<ReportFilesResponseDTO> files(
            @AuthenticationPrincipal UserDTO principal,   // ✅ 변경
            @PathVariable Long reportId
    ) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        ReportPrincipal rp = toReportPrincipal(principal);
        return ResponseEntity.ok(reportService.getFiles(rp, reportId));
    }

    // 4. 리포트 다운로드 로그
    @GetMapping("/{reportId}/downloads")
    public ReportDownloadLogsResponseDTO downloadLogsByReport(
            @PathVariable Long reportId,
            @AuthenticationPrincipal UserDTO principal     // ✅ 변경
    ) {

        ReportPrincipal rp = toReportPrincipal(principal);
        return reportService.getDownloadLogsByReportId(rp, reportId);

    }

    // 5. 디버그
    @GetMapping("/debug/me")
    public Object me(@AuthenticationPrincipal Object principal) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return java.util.Map.of(
                "principalParam", principal,
                "principalParamClass", principal == null ? null : principal.getClass().getName(),
                "auth", auth == null ? null : auth.toString(),
                "authPrincipalClass", auth == null ? null : auth.getPrincipal().getClass().getName(),
                "authorities", auth == null ? null : auth.getAuthorities()
        );
    }
}
