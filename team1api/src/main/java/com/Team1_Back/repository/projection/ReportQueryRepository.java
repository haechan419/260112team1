package com.Team1_Back.repository.projection;

import com.Team1_Back.domain.Expense;
import com.Team1_Back.repository.ApprovedAgg;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
public interface ReportQueryRepository extends JpaRepository<Expense, Long> {

    @Query(value = """
        SELECT COALESCE(SUM(e.amount), 0) AS total,
               COUNT(*) AS cnt
        FROM expense e
        WHERE e.approval_status = 'APPROVED'
          AND e.receipt_date >= :start
          AND e.receipt_date <= :end
        """, nativeQuery = true)
    ApprovedAgg approvedSumAll(@Param("start") LocalDate start,
                               @Param("end") LocalDate end);

    @Query(value = """
        SELECT COALESCE(SUM(e.amount), 0) AS total,
               COUNT(*) AS cnt
        FROM expense e
        WHERE e.approval_status = 'APPROVED'
          AND e.receipt_date >= :start
          AND e.receipt_date <= :end
          AND e.user_id = :userId
        """, nativeQuery = true)
    ApprovedAgg approvedSumByUser(@Param("userId") Long userId,
                                  @Param("start") LocalDate start,
                                  @Param("end") LocalDate end);

    @Query(value = """
        SELECT COALESCE(SUM(e.amount), 0) AS total,
               COUNT(*) AS cnt
        FROM expense e
        JOIN users u ON u.id = e.user_id
        WHERE e.approval_status = 'APPROVED'
          AND e.receipt_date >= :start
          AND e.receipt_date <= :end
          AND TRIM(u.department_name) = TRIM(:dept)
        """, nativeQuery = true)
    ApprovedAgg approvedSumByDept(@Param("dept") String dept,
                                  @Param("start") LocalDate start,
                                  @Param("end") LocalDate end);
}
