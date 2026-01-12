package com.Team1_Back.repository;

import com.Team1_Back.domain.DepartmentPerformance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 부서별 실적 Repository
 * AI 분석용 조회 메서드 제공
 */
@Repository
public interface DepartmentPerformanceRepository extends JpaRepository<DepartmentPerformance, Long> {

    /**
     * 특정 부서의 연도별 실적 조회
     */
    List<DepartmentPerformance> findByDepartmentNameAndYearOrderByMonth(String departmentName, Integer year);

    /**
     * 여러 부서의 특정 연도 실적 조회 (비교용)
     */
    @Query("SELECT dp FROM DepartmentPerformance dp " +
           "WHERE dp.departmentName IN :departments AND dp.year = :year " +
           "ORDER BY dp.departmentName, dp.month")
    List<DepartmentPerformance> findByDepartmentsAndYear(
        @Param("departments") List<String> departments,
        @Param("year") Integer year
    );

    /**
     * 특정 연도의 모든 부서 실적 조회
     */
    List<DepartmentPerformance> findByYearOrderByDepartmentNameAscMonthAsc(Integer year);

    /**
     * 특정 부서, 연도, 월 범위 조회
     */
    @Query("SELECT dp FROM DepartmentPerformance dp " +
           "WHERE dp.departmentName IN :departments " +
           "AND dp.year = :year " +
           "AND dp.month BETWEEN :startMonth AND :endMonth " +
           "ORDER BY dp.departmentName, dp.month")
    List<DepartmentPerformance> findByDepartmentsAndYearAndMonthRange(
        @Param("departments") List<String> departments,
        @Param("year") Integer year,
        @Param("startMonth") Integer startMonth,
        @Param("endMonth") Integer endMonth
    );

    /**
     * 모든 부서명 목록 조회
     */
    @Query("SELECT DISTINCT dp.departmentName FROM DepartmentPerformance dp ORDER BY dp.departmentName")
    List<String> findAllDepartmentNames();
}

