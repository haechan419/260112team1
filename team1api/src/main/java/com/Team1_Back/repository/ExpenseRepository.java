package com.Team1_Back.repository;

import com.Team1_Back.domain.ApprovalStatus;
import com.Team1_Back.domain.Expense;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    // 사용자별 지출 내역 조회 (페이징)
    @EntityGraph(attributePaths = { "writer" })
    @Query("SELECT e FROM Expense e WHERE e.writer.id = :writerId")
    Page<Expense> findByWriterId(@Param("writerId") Long writerId, Pageable pageable);

    // 사용자별 + 상태별 지출 내역 조회
    @EntityGraph(attributePaths = { "writer" })
    @Query("SELECT e FROM Expense e WHERE e.writer.id = :writerId AND e.status = :status")
    Page<Expense> findByWriterIdAndStatus(@Param("writerId") Long writerId, @Param("status") ApprovalStatus status,
            Pageable pageable);

    // 사용자별 + 기간별 지출 내역 조회 (상신일 기준: createdAt)
    @EntityGraph(attributePaths = { "writer" })
    @Query("SELECT e FROM Expense e WHERE e.writer.id = :userId " +
            "AND DATE(e.createdAt) BETWEEN :startDate AND :endDate")
    Page<Expense> findByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable);

    // 사용자별 + 상태별 + 기간별 지출 내역 조회 (상신일 기준: createdAt)
    @EntityGraph(attributePaths = { "writer" })
    @Query("SELECT e FROM Expense e WHERE e.writer.id = :userId " +
            "AND e.status = :status " +
            "AND DATE(e.createdAt) BETWEEN :startDate AND :endDate")
    Page<Expense> findByUserIdAndStatusAndDateRange(
            @Param("userId") Long userId,
            @Param("status") ApprovalStatus status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable);

    // 사용자별 + 기간별 지출 내역 조회 - 시작일만 (상신일 기준: createdAt)
    @EntityGraph(attributePaths = { "writer" })
    @Query("SELECT e FROM Expense e WHERE e.writer.id = :userId " +
            "AND DATE(e.createdAt) >= :startDate")
    Page<Expense> findByUserIdAndStartDate(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            Pageable pageable);

    // 사용자별 + 기간별 지출 내역 조회 - 종료일만 (상신일 기준: createdAt)
    @EntityGraph(attributePaths = { "writer" })
    @Query("SELECT e FROM Expense e WHERE e.writer.id = :userId " +
            "AND DATE(e.createdAt) <= :endDate")
    Page<Expense> findByUserIdAndEndDate(
            @Param("userId") Long userId,
            @Param("endDate") LocalDate endDate,
            Pageable pageable);

    // 사용자별 + 상태별 + 시작일만 (상신일 기준: createdAt)
    @EntityGraph(attributePaths = { "writer" })
    @Query("SELECT e FROM Expense e WHERE e.writer.id = :userId " +
            "AND e.status = :status " +
            "AND DATE(e.createdAt) >= :startDate")
    Page<Expense> findByUserIdAndStatusAndStartDate(
            @Param("userId") Long userId,
            @Param("status") ApprovalStatus status,
            @Param("startDate") LocalDate startDate,
            Pageable pageable);

    // 사용자별 + 상태별 + 종료일만 (상신일 기준: createdAt)
    @EntityGraph(attributePaths = { "writer" })
    @Query("SELECT e FROM Expense e WHERE e.writer.id = :userId " +
            "AND e.status = :status " +
            "AND DATE(e.createdAt) <= :endDate")
    Page<Expense> findByUserIdAndStatusAndEndDate(
            @Param("userId") Long userId,
            @Param("status") ApprovalStatus status,
            @Param("endDate") LocalDate endDate,
            Pageable pageable);

    // 사용자 ID와 지출 ID로 조회 (권한 확인용)
    @EntityGraph(attributePaths = { "writer" })
    @Query("SELECT e FROM Expense e WHERE e.id = :id AND e.writer.id = :writerId")
    Optional<Expense> findByIdAndWriterId(@Param("id") Long id, @Param("writerId") Long writerId);

    // 관리자용: 지출 ID로 조회 (writer 정보 포함)
    @EntityGraph(attributePaths = { "writer" })
    @Query("SELECT e FROM Expense e WHERE e.id = :id")
    Optional<Expense> findByIdWithWriter(@Param("id") Long id);

    // 상태별 전체 조회 (관리자용)
    @EntityGraph(attributePaths = { "writer" })
    Page<Expense> findByStatus(ApprovalStatus status, Pageable pageable);

    // 전체 조회 (관리자용, DRAFT 제외용)
    @EntityGraph(attributePaths = { "writer" })
    @Query("SELECT e FROM Expense e WHERE e.status != com.Team1_Back.domain.ApprovalStatus.DRAFT")
    Page<Expense> findAllSubmitted(Pageable pageable);

    // 부서별 지출 집계 (mallapi 패턴: native query 사용)
    @Query(value = "SELECT u.department_name as departmentName, " +
            "       COUNT(e.id) as expenseCount, " +
            "       COALESCE(SUM(e.amount), 0) as totalAmount " +
            "FROM expense e " +
            "JOIN users u ON e.user_id = u.id " +
            "WHERE e.approval_status = :status " +
            "  AND u.department_name IS NOT NULL " +
            "GROUP BY u.department_name", nativeQuery = true)
    List<Object[]> findDepartmentStatistics(@Param("status") String status);

    // 카테고리별 지출 집계 (mallapi 패턴: native query 사용)
    @Query(value = "SELECT e.category as categoryName, " +
            "       COUNT(e.id) as expenseCount, " +
            "       COALESCE(SUM(e.amount), 0) as totalAmount " +
            "FROM expense e " +
            "WHERE e.approval_status = :status " +
            "  AND e.category IS NOT NULL " +
            "GROUP BY e.category", nativeQuery = true)
    List<Object[]> findCategoryStatistics(@Param("status") String status);

    // 오늘의 미결재 건수 조회
    @Query(value = "SELECT COUNT(*) " +
            "FROM expense e " +
            "WHERE e.approval_status = 'SUBMITTED' " +
            "  AND DATE(e.created_at) = CURDATE()", nativeQuery = true)
    Long countTodayPending();

    // 이번 달 총 지출액 조회
    @Query(value = "SELECT COALESCE(SUM(e.amount), 0) " +
            "FROM expense e " +
            "WHERE e.approval_status = :status " +
            "  AND YEAR(e.receipt_date) = YEAR(CURDATE()) " +
            "  AND MONTH(e.receipt_date) = MONTH(CURDATE())", nativeQuery = true)
    Long sumMonthlyTotalExpense(@Param("status") String status);

    // 특정 년월의 총 지출액 조회 (전월 대비 계산용)
    @Query(value = "SELECT COALESCE(SUM(e.amount), 0) " +
            "FROM expense e " +
            "WHERE e.approval_status = :status " +
            "  AND DATE_FORMAT(e.receipt_date, '%Y-%m') = :yearMonth", nativeQuery = true)
    Long sumMonthlyTotalExpenseByYearMonth(
            @Param("yearMonth") String yearMonth,
            @Param("status") String status);

    // 리포트용: 기간별 + 스코프별 지출 내역 조회 (페이징 없이 전체 조회)
    @EntityGraph(attributePaths = { "writer" })
    @Query("SELECT e FROM Expense e WHERE " +
            "e.receiptDate BETWEEN :startDate AND :endDate " +
            "AND e.status = com.Team1_Back.domain.ApprovalStatus.APPROVED")
    List<Expense> findForReportByDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // 리포트용: 사용자별 + 기간별 지출 내역 조회
    @EntityGraph(attributePaths = { "writer" })
    @Query("SELECT e FROM Expense e WHERE " +
            "e.writer.id = :userId " +
            "AND e.receiptDate BETWEEN :startDate AND :endDate " +
            "AND e.status = com.Team1_Back.domain.ApprovalStatus.APPROVED")
    List<Expense> findForReportByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // 리포트용: 부서별 + 기간별 지출 내역 조회
    @EntityGraph(attributePaths = { "writer" })
    @Query("SELECT e FROM Expense e WHERE " +
            "e.writer.departmentName = :departmentName " +
            "AND e.receiptDate BETWEEN :startDate AND :endDate " +
            "AND e.status = com.Team1_Back.domain.ApprovalStatus.APPROVED")
    List<Expense> findForReportByDepartmentAndDateRange(
            @Param("departmentName") String departmentName,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // 한해찬 추가
    // 월별 지출 조회
    @Query(value = "SELECT DATE_FORMAT(e.receipt_date, '%Y-%m') as yearMonth, " +
            "       COALESCE(SUM(e.amount), 0) as totalAmount " +
            "FROM expense e " +
            "WHERE e.approval_status = :status " +
            "  AND e.receipt_date >= DATE_SUB(CURDATE(), INTERVAL 12 MONTH) " +
            "GROUP BY DATE_FORMAT(e.receipt_date, '%Y-%m') " +
            "ORDER BY yearMonth ASC", nativeQuery = true)
    List<Object[]> findMonthlyExpenseTrend(@Param("status") String status);
}
