package com.Team1_Back.repository;

import com.Team1_Back.domain.UserBudgetMonthly;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 사용자별 월간 예산 Repository 인터페이스
 * 
 * <p>⚠️ 주의: 이 Repository는 회계통계 기능에서 사용됩니다.
 * 
 * <p>사용 위치:
 * <ul>
 *   <li>AccountingServiceImpl.getSummary() - 예산 집행률 계산, 예산 초과 인원 수 조회</li>
 *   <li>AccountingServiceImpl.getOverBudgetList() - 예산 초과 인원 리스트 조회</li>
 * </ul>
 * 
 * @author Team1
 */
public interface UserBudgetMonthlyRepository extends JpaRepository<UserBudgetMonthly, Long> {

    /**
     * 예산 초과 인원 조회 (예산의 80% 이상 사용한 인원)
     * 
     * <p>반환되는 Object[] 배열 구조:
     * <ul>
     *   <li>row[0] - user_id (Long)</li>
     *   <li>row[1] - name (String)</li>
     *   <li>row[2] - department_name (String)</li>
     *   <li>row[3] - monthly_limit (Long)</li>
     *   <li>row[4] - total_expense (Long)</li>
     *   <li>row[5] - remaining (Long) - 남은 예산</li>
     * </ul>
     * 
     * @param yearMonth 년월 (YYYY-MM 형식)
     * @return 예산 초과 인원 목록
     */
    @Query(value = 
        "SELECT ubm.user_id, " +
        "       u.name, " +
        "       u.department_name, " +
        "       ubm.monthly_limit, " +
        "       COALESCE(SUM(e.amount), 0) as total_expense, " +
        "       (ubm.monthly_limit - COALESCE(SUM(e.amount), 0)) as remaining " +
        "FROM user_budget_monthly ubm " +
        "JOIN users u ON ubm.user_id = u.id " +
        "LEFT JOIN expense e ON e.user_id = u.id " +
        "  AND e.approval_status = 'APPROVED' " +
        "  AND DATE_FORMAT(e.receipt_date, '%Y-%m') = ubm.year_month " +
        "WHERE ubm.year_month = :yearMonth " +
        "GROUP BY ubm.user_id, u.name, u.department_name, ubm.monthly_limit " +
        "HAVING (COALESCE(SUM(e.amount), 0) / ubm.monthly_limit * 100) >= 80 " +
        "ORDER BY (COALESCE(SUM(e.amount), 0) / ubm.monthly_limit * 100) DESC",
        nativeQuery = true)
    List<Object[]> findOverBudgetUsers(@Param("yearMonth") String yearMonth);

    /**
     * 사용자 ID와 년월로 예산 조회
     * 
     * @param userId 사용자 ID
     * @param yearMonth 년월 (YYYY-MM 형식)
     * @return 예산 정보
     */
    @Query("SELECT ubm FROM UserBudgetMonthly ubm WHERE ubm.user.id = :userId AND ubm.yearMonth = :yearMonth")
    Optional<UserBudgetMonthly> findByUserIdAndYearMonth(@Param("userId") Long userId, @Param("yearMonth") String yearMonth);
}

