package com.Team1_Back.controller;

import com.Team1_Back.dto.UserDTO;
import com.Team1_Back.dto.ReportScheduleCreateRequestDTO;
import com.Team1_Back.dto.ReportScheduleResponseDTO;
import com.Team1_Back.dto.ReportScheduleUpsertRequestDTO;
import com.Team1_Back.dto.ReportSchedulesResponseDTO;
import com.Team1_Back.service.ReportScheduleAdminService;
import com.Team1_Back.service.ReportService;
import com.Team1_Back.security.ReportPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
@RestController
@RequestMapping("/api/admin/report-schedules")
@RequiredArgsConstructor
public class ReportScheduleAdminController {

    private final ReportService reportService;
    private final ReportScheduleAdminService scheduleService;

    private ReportPrincipal toReportPrincipal(UserDTO user) {
        if (user == null) return null;

        boolean isAdmin = user.getRoleNames() != null &&
                user.getRoleNames().stream().anyMatch(r ->
                        "ADMIN".equalsIgnoreCase(r) || "ROLE_ADMIN".equalsIgnoreCase(r)
                );

        String role = isAdmin ? "ADMIN" : "USER";
        return new ReportPrincipal(user.getId(), role, user.getDepartmentName());
    }

    @GetMapping
    public ReportSchedulesResponseDTO list(@AuthenticationPrincipal UserDTO user) {
        if (user == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return reportService.getSchedules(toReportPrincipal(user));
    }

    @PostMapping
    public ResponseEntity<ReportScheduleResponseDTO> create(
            @AuthenticationPrincipal UserDTO user,
            @Valid @RequestBody ReportScheduleCreateRequestDTO req
    ) {
        if (user == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return ResponseEntity.ok(scheduleService.create(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReportScheduleResponseDTO> update(
            @AuthenticationPrincipal UserDTO user,
            @PathVariable Long id,
            @Valid @RequestBody ReportScheduleUpsertRequestDTO req
    ) {
        if (user == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return ResponseEntity.ok(scheduleService.update(id, req));
    }

    @PostMapping("/{id}/run")
    public ResponseEntity<ReportScheduleResponseDTO> runNow(
            @AuthenticationPrincipal UserDTO user,
            @PathVariable Long id
    ) {
        if (user == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return ResponseEntity.ok(scheduleService.runNow(id));
    }
}
