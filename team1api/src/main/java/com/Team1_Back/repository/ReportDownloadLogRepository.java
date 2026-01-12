package com.Team1_Back.repository;

import com.Team1_Back.domain.ReportDownloadLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportDownloadLogRepository extends JpaRepository<ReportDownloadLog, Long> {

    List<ReportDownloadLog> findByReportJob_IdOrderByIdDesc(Long reportId);

    // reportId(=report_job.id) 기준으로 로그 조회
    List<ReportDownloadLog> findByReportFile_ReportJob_IdOrderByIdDesc(Long reportJobId);

    // fileId 기준 조회도 필요하면
    List<ReportDownloadLog> findByReportFile_IdOrderByIdDesc(Long reportFileId);
}
