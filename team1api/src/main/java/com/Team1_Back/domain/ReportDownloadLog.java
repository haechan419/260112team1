package com.Team1_Back.domain;

import com.Team1_Back.report.entity.ReportJob;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter
@Entity
@Table(name = "report_download_log", indexes = {
        @Index(name = "idx_rdl_file", columnList = "report_file_id"),
        @Index(name = "idx_rdl_by", columnList = "downloaded_by")
})
public class ReportDownloadLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_job_id")
    private ReportJob reportJob;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_file_id", nullable = false)
    private ReportFile reportFile;

    @Column(name = "downloaded_by", nullable = false)
    private Long downloadedBy;

    // DB DEFAULT CURRENT_TIMESTAMP 사용 (insertable=false, updatable=false)
    @Column(name = "downloaded_at", insertable = false, updatable = false)
    private LocalDateTime downloadedAt;
}
