package com.Team1_Back.repository;

import com.Team1_Back.domain.ReportFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReportFileRepository extends JpaRepository<ReportFile, Long> {

    Optional<ReportFile> findByChecksum(String checksum);

    // 최신 1개
    Optional<ReportFile> findTopByReportJob_IdOrderByIdDesc(Long reportJobId);

    // 목록
    List<ReportFile> findByReportJob_IdOrderByCreatedAtDesc(Long reportJobId);
}

