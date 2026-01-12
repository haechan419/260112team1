package com.Team1_Back.controller;

import com.Team1_Back.dto.DepartmentAttendanceDTO;
import com.Team1_Back.service.AttendanceAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/attendance")
@Slf4j
public class AttendanceAdminController {

    private final AttendanceAdminService attendanceAdminService;

    /**
     * 부서별 출결 통계 조회
     * GET /api/admin/attendance?year=2025&month=1&department=개발1팀
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAttendanceList(
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(required = false) String department
    ) {
        try {
            log.info("출결 조회 요청 - year: {}, month: {}, department: {}", year, month, department);

            List<DepartmentAttendanceDTO> list = attendanceAdminService.getAttendanceByDepartment(year, month, department);

            log.info("출결 조회 성공 - 결과: {}건", list.size());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", list,
                    "year", year,
                    "month", month
            ));
        } catch (Exception e) {
            log.error("출결 조회 실패 - year: {}, month: {}, department: {}", year, month, department, e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "출결 조회 중 오류가 발생했습니다: " + e.getMessage()
            ));
        }
    }

    /**
     * 모든 부서 목록 조회
     * GET /api/admin/attendance/departments
     */
    @GetMapping("/departments")
    public ResponseEntity<Map<String, Object>> getDepartments() {
        List<String> departments = attendanceAdminService.getAllDepartments();

        return ResponseEntity.ok(Map.of(
                "success", true,
                "departments", departments
        ));
    }

    /**
     * 부서별 출결 상세 조회
     * GET /api/admin/attendance/detail?year=2025&month=1&department=개발1팀
     */
    @GetMapping("/detail")
    public ResponseEntity<Map<String, Object>> getAttendanceDetail(
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam String department
    ) {
        log.info("출결 상세 조회 - year: {}, month: {}, department: {}", year, month, department);

        DepartmentAttendanceDTO detail = attendanceAdminService.getAttendanceDetail(year, month, department);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", detail
        ));
    }

    /**
     * 부서별 출결 엑셀 다운로드
     * GET /api/admin/attendance/download?year=2025&month=1&department=개발1팀
     */
    @GetMapping("/download")
    public ResponseEntity<?> downloadAttendanceExcel(
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam String department
    ) {
        log.info("출결 엑셀 다운로드 요청 - year: {}, month: {}, department: {}", year, month, department);

        try {
            byte[] excelData = attendanceAdminService.generateAttendanceExcel(year, month, department);

            if (excelData == null || excelData.length == 0) {
                log.warn("엑셀 데이터가 비어있음 - year: {}, month: {}, department: {}", year, month, department);
                return ResponseEntity.status(404).body(Map.of(
                        "success", false,
                        "message", "출결 데이터가 없습니다."
                ));
            }

            String fileName = year + "년_" + month + "월_" + department + "_출결현황.xlsx";
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            // ✅ Content-Disposition 헤더는 한 번만 설정 (중복 방지)
            headers.add("Content-Disposition", "attachment; filename=\"" + fileName + "\"; filename*=UTF-8''" + encodedFileName);

            log.info("엑셀 다운로드 성공 - 파일명: {}, 크기: {} bytes", fileName, excelData.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelData);

        } catch (IllegalArgumentException e) {
            log.error("엑셀 생성 실패 - 잘못된 파라미터: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "잘못된 요청입니다: " + e.getMessage()
            ));
        } catch (Exception e) {
            log.error("엑셀 생성 실패 - year: {}, month: {}, department: {}", year, month, department, e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "엑셀 파일 생성 중 오류가 발생했습니다: " + e.getMessage()
            ));
        }
    }
}