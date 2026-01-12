package com.Team1_Back.repository;

import com.Team1_Back.domain.Attendance;
import com.Team1_Back.domain.AttendanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    Optional<Attendance> findByUserIdAndAttendanceDate(Long userId, LocalDate date);

    List<Attendance> findByUserIdAndAttendanceDateBetween(Long userId, LocalDate start, LocalDate end);

    // ✅ 추가: 부서별 + 기간별 출결 조회
    @Query("SELECT a FROM Attendance a JOIN FETCH a.user u " +
            "WHERE u.departmentName = :department " +
            "AND a.attendanceDate BETWEEN :startDate AND :endDate " +
            "ORDER BY a.attendanceDate, u.name")
    List<Attendance> findByDepartmentAndDateRange(
            @Param("department") String department,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    // ✅ 추가: 전체 부서 + 기간별 출결 조회
    @Query("SELECT a FROM Attendance a JOIN FETCH a.user u " +
            "WHERE a.attendanceDate BETWEEN :startDate AND :endDate " +
            "ORDER BY u.departmentName, a.attendanceDate, u.name")
    List<Attendance> findByDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    // ✅ 추가: 특정 날짜의 출결 기록이 없는 사용자 조회용 (결근 처리)
    @Query("SELECT a FROM Attendance a WHERE a.attendanceDate = :date")
    List<Attendance> findByAttendanceDate(@Param("date") LocalDate date);

    // ✅ 추가: 부서별 + 특정 날짜 + 상태별 카운트
    @Query("SELECT COUNT(a) FROM Attendance a JOIN a.user u " +
            "WHERE u.departmentName = :department " +
            "AND a.attendanceDate = :date " +
            "AND a.status = :status")
    Long countByDepartmentAndDateAndStatus(
            @Param("department") String department,
            @Param("date") LocalDate date,
            @Param("status") AttendanceStatus status
    );
}