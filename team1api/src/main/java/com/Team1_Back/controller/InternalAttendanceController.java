package com.Team1_Back.controller;

import com.Team1_Back.dto.DepartmentAttendanceDTO;
import com.Team1_Back.service.AttendanceAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Python 서버 전용 내부 API 컨트롤러
 * 인증 없이 접근 가능 (SecurityConfig에서 permitAll 설정)
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/internal/attendance")
@Slf4j
public class InternalAttendanceController {

    private final AttendanceAdminService attendanceAdminService;

    /**
     * Python 서버 전용 - 부서별 출결 상세 조회 (인증 불필요)
     * GET /api/internal/attendance/detail?year=2025&month=1&department=개발1팀
     */
    @GetMapping("/detail")
    public ResponseEntity<Map<String, Object>> getAttendanceDetail(
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam String department
    ) {
        log.info("[Internal API] 출결 상세 조회 - year: {}, month: {}, department: {}", year, month, department);

        DepartmentAttendanceDTO detail = attendanceAdminService.getAttendanceDetail(year, month, department);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", detail
        ));
    }
}


