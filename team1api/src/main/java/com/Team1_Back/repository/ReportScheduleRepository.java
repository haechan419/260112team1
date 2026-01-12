package com.Team1_Back.repository;

import com.Team1_Back.domain.ReportSchedule;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface ReportScheduleRepository extends JpaRepository<ReportSchedule, Long> {

    List<ReportSchedule> findByIsEnabledTrueAndNextRunAtLessThanEqualOrderByNextRunAtAsc(
            LocalDateTime now,
            Pageable pageable
    );

    @Query("select s from ReportSchedule s order by s.isEnabled desc, s.nextRunAt asc nulls last, s.id desc")
    List<ReportSchedule> findAllForAdmin();
}
