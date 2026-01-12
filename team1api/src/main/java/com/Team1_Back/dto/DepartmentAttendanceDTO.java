package com.Team1_Back.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepartmentAttendanceDTO {

    private String department;       // 부서명
    private int year;                // 연도
    private int month;               // 월
    private int totalEmployees;      // 부서 총 인원
    private int presentCount;        // 출근 수
    private int lateCount;           // 지각 수
    private int absentCount;         // 결근 수
    private int leaveCount;          // 휴가 수
    private String createdAt;        // 생성일시
    private String fileName;         // 엑셀 파일명 (있을 경우)
    private List<AttendanceDetailDTO> details;  // 상세 내역

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AttendanceDetailDTO {
        private String employeeNo;    // 사번
        private String employeeName;  // 이름
        private String date;          // 날짜
        private String status;        // 상태 (PRESENT, LATE, ABSENT, LEAVE)
        private String statusKorean;  // 상태 한글 (출근, 지각, 결근, 휴가)
        private String checkInTime;   // 출근 시간
        private String checkOutTime;  // 퇴근 시간
    }
}