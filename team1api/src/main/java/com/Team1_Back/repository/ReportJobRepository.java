package com.Team1_Back.repository;

import com.Team1_Back.report.entity.ReportJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ReportJobRepository extends JpaRepository<ReportJob, Long> {

    Optional<ReportJob> findById(Long id);

    @Query(
            value = """
            SELECT DISTINCT department_name
            FROM users
            WHERE department_name IS NOT NULL
              AND TRIM(department_name) <> ''
            ORDER BY department_name ASC
        """,
            nativeQuery = true
    )
    List<String> findDistinctDepartmentNames();

}
