package com.Team1_Back.service;

import com.Team1_Back.dto.DepartmentAttendanceDTO;

import java.util.List;

public interface AttendanceAdminService {

    // 부서별 출결 통계 조회
    List<DepartmentAttendanceDTO> getAttendanceByDepartment(int year, int month, String department);

    // 모든 부서 목록 조회
    List<String> getAllDepartments();

    // 부서별 월별 출결 상세 조회
    DepartmentAttendanceDTO getAttendanceDetail(int year, int month, String department);

    // 부서별 월별 출결 엑셀 생성
    byte[] generateAttendanceExcel(int year, int month, String department);
}