package com.Team1_Back.service;

import com.Team1_Back.domain.Attendance;
import com.Team1_Back.domain.AttendanceStatus;
import com.Team1_Back.repository.AttendanceRepository;
import com.Team1_Back.repository.UserRepository;
import com.Team1_Back.dto.DepartmentAttendanceDTO;
import com.Team1_Back.dto.DepartmentAttendanceDTO.AttendanceDetailDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AttendanceAdminServiceImpl implements AttendanceAdminService {

    private final AttendanceRepository attendanceRepository;
    private final UserRepository userRepository;

    @Override
    public List<DepartmentAttendanceDTO> getAttendanceByDepartment(int year, int month, String department) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        log.info("출결 조회 - year: {}, month: {}, department: {}, 기간: {} ~ {}", 
                year, month, department, startDate, endDate);

        List<Attendance> attendances;

        if (department == null || department.isEmpty()) {
            // 전체 부서 조회
            attendances = attendanceRepository.findByDateRange(startDate, endDate);
        } else {
            // 특정 부서 조회
            attendances = attendanceRepository.findByDepartmentAndDateRange(department, startDate, endDate);
        }

        log.info("조회된 출결 데이터: {}건", attendances.size());

        // 부서명이 null인 데이터 확인
        long nullDeptCount = attendances.stream()
                .filter(a -> a.getUser() == null || a.getUser().getDepartmentName() == null || a.getUser().getDepartmentName().isEmpty())
                .count();
        if (nullDeptCount > 0) {
            log.warn("⚠️  부서명이 없는 출결 데이터: {}건 (필터링됨)", nullDeptCount);
        }

        // 부서별로 그룹핑
        Map<String, List<Attendance>> byDepartment = attendances.stream()
                .filter(a -> a.getUser() != null && a.getUser().getDepartmentName() != null && !a.getUser().getDepartmentName().isEmpty())
                .collect(Collectors.groupingBy(a -> a.getUser().getDepartmentName()));

        log.info("부서별 그룹핑 결과: {}개 부서", byDepartment.size());
        
        // 부서별 상세 로그
        byDepartment.forEach((dept, list) -> 
                log.info("  - {}: {}건", dept, list.size()));

        List<DepartmentAttendanceDTO> result = new ArrayList<>();

        for (Map.Entry<String, List<Attendance>> entry : byDepartment.entrySet()) {
            String deptName = entry.getKey();
            List<Attendance> deptAttendances = entry.getValue();

            // 통계 계산
            long presentCount = deptAttendances.stream()
                    .filter(a -> a.getStatus() == AttendanceStatus.PRESENT).count();
            long lateCount = deptAttendances.stream()
                    .filter(a -> a.getStatus() == AttendanceStatus.LATE).count();
            long absentCount = deptAttendances.stream()
                    .filter(a -> a.getStatus() == AttendanceStatus.ABSENT).count();
            long leaveCount = deptAttendances.stream()
                    .filter(a -> a.getStatus() == AttendanceStatus.LEAVE).count();

            // 부서 인원 수 (중복 제거)
            long totalEmployees = deptAttendances.stream()
                    .map(a -> a.getUser().getId())
                    .distinct()
                    .count();

            DepartmentAttendanceDTO dto = DepartmentAttendanceDTO.builder()
                    .department(deptName)
                    .year(year)
                    .month(month)
                    .totalEmployees((int) totalEmployees)
                    .presentCount((int) presentCount)
                    .lateCount((int) lateCount)
                    .absentCount((int) absentCount)
                    .leaveCount((int) leaveCount)
                    .createdAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                    .fileName(year + "년_" + month + "월_" + deptName + "_출결현황.xlsx")
                    .build();

            result.add(dto);
        }

        // 부서명 정렬
        result.sort(Comparator.comparing(DepartmentAttendanceDTO::getDepartment));

        return result;
    }

    @Override
    public List<String> getAllDepartments() {
        return userRepository.findAll().stream()
                .map(user -> user.getDepartmentName())
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    @Override
    public DepartmentAttendanceDTO getAttendanceDetail(int year, int month, String department) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        List<Attendance> attendances = attendanceRepository.findByDepartmentAndDateRange(
                department, startDate, endDate);

        // 상세 내역 생성
        List<AttendanceDetailDTO> details = attendances.stream()
                .map(a -> AttendanceDetailDTO.builder()
                        .employeeNo(a.getUser().getEmployeeNo())
                        .employeeName(a.getUser().getName())
                        .date(a.getAttendanceDate().toString())
                        .status(a.getStatus().name())
                        .statusKorean(getStatusKorean(a.getStatus()))
                        .checkInTime(a.getCheckInTime() != null ?
                                a.getCheckInTime().format(DateTimeFormatter.ofPattern("HH:mm")) : "-")
                        .checkOutTime(a.getCheckOutTime() != null ?
                                a.getCheckOutTime().format(DateTimeFormatter.ofPattern("HH:mm")) : "-")
                        .build())
                .collect(Collectors.toList());

        // 통계 계산
        long presentCount = attendances.stream()
                .filter(a -> a.getStatus() == AttendanceStatus.PRESENT).count();
        long lateCount = attendances.stream()
                .filter(a -> a.getStatus() == AttendanceStatus.LATE).count();
        long absentCount = attendances.stream()
                .filter(a -> a.getStatus() == AttendanceStatus.ABSENT).count();
        long leaveCount = attendances.stream()
                .filter(a -> a.getStatus() == AttendanceStatus.LEAVE).count();

        long totalEmployees = attendances.stream()
                .map(a -> a.getUser().getId())
                .distinct()
                .count();

        return DepartmentAttendanceDTO.builder()
                .department(department)
                .year(year)
                .month(month)
                .totalEmployees((int) totalEmployees)
                .presentCount((int) presentCount)
                .lateCount((int) lateCount)
                .absentCount((int) absentCount)
                .leaveCount((int) leaveCount)
                .createdAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .fileName(year + "년_" + month + "월_" + department + "_출결현황.xlsx")
                .details(details)
                .build();
    }

    @Override
    public byte[] generateAttendanceExcel(int year, int month, String department) {
        DepartmentAttendanceDTO data = getAttendanceDetail(year, month, department);

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("출결현황");

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle titleStyle = createTitleStyle(workbook);

            int rowNum = 0;

            // 제목
            Row titleRow = sheet.createRow(rowNum++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(department + " " + year + "년 " + month + "월 출결 현황");
            titleCell.setCellStyle(titleStyle);

            rowNum++; // 빈 행

            // 통계 요약
            Row summaryRow = sheet.createRow(rowNum++);
            summaryRow.createCell(0).setCellValue("총 인원");
            summaryRow.createCell(1).setCellValue(data.getTotalEmployees() + "명");
            summaryRow.createCell(2).setCellValue("출근");
            summaryRow.createCell(3).setCellValue(data.getPresentCount() + "건");
            summaryRow.createCell(4).setCellValue("지각");
            summaryRow.createCell(5).setCellValue(data.getLateCount() + "건");

            Row summaryRow2 = sheet.createRow(rowNum++);
            summaryRow2.createCell(0).setCellValue("생성일시");
            summaryRow2.createCell(1).setCellValue(data.getCreatedAt());
            summaryRow2.createCell(2).setCellValue("결근");
            summaryRow2.createCell(3).setCellValue(data.getAbsentCount() + "건");
            summaryRow2.createCell(4).setCellValue("휴가");
            summaryRow2.createCell(5).setCellValue(data.getLeaveCount() + "건");

            rowNum++; // 빈 행

            // 상세 테이블 헤더
            Row headerRow = sheet.createRow(rowNum++);
            String[] headers = {"사번", "이름", "날짜", "상태", "출근시간", "퇴근시간"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // 상세 데이터
            if (data.getDetails() != null) {
                for (AttendanceDetailDTO detail : data.getDetails()) {
                    Row dataRow = sheet.createRow(rowNum++);

                    Cell cell0 = dataRow.createCell(0);
                    cell0.setCellValue(detail.getEmployeeNo());
                    cell0.setCellStyle(dataStyle);

                    Cell cell1 = dataRow.createCell(1);
                    cell1.setCellValue(detail.getEmployeeName());
                    cell1.setCellStyle(dataStyle);

                    Cell cell2 = dataRow.createCell(2);
                    cell2.setCellValue(detail.getDate());
                    cell2.setCellStyle(dataStyle);

                    Cell cell3 = dataRow.createCell(3);
                    cell3.setCellValue(detail.getStatusKorean());
                    cell3.setCellStyle(dataStyle);

                    Cell cell4 = dataRow.createCell(4);
                    cell4.setCellValue(detail.getCheckInTime());
                    cell4.setCellStyle(dataStyle);

                    Cell cell5 = dataRow.createCell(5);
                    cell5.setCellValue(detail.getCheckOutTime());
                    cell5.setCellStyle(dataStyle);
                }
            }

            // 컬럼 너비 자동 조정
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                if (sheet.getColumnWidth(i) < 3000) {
                    sheet.setColumnWidth(i, 3000);
                }
            }

            workbook.write(out);
            return out.toByteArray();

        } catch (Exception e) {
            log.error("엑셀 생성 중 오류", e);
            throw new RuntimeException("엑셀 파일 생성 실패", e);
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        style.setFont(font);
        return style;
    }

    private String getStatusKorean(AttendanceStatus status) {
        return switch (status) {
            case PRESENT -> "출근";
            case LATE -> "지각";
            case ABSENT -> "결근";
            case LEAVE -> "휴가";
        };
    }
}