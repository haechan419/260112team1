package com.Team1_Back.domain;

import com.Team1_Back.report.entity.ReportJob;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter
@Entity
@Table(name="report_file", indexes = {
        @Index(name="idx_report_file_job", columnList="report_job_id")
})
public class ReportFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="report_file_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name="report_job_id", nullable=false)
    private ReportJob reportJob;

    @Column(name="file_name", nullable=false, length=255)
    private String fileName;

    @Column(name="file_url", nullable=false, length=500)
    private String fileUrl;

    @Column(name="file_type", nullable=false, length=20)
    private String fileType; // "PDF" / "EXCEL"

    @Column(name="file_size", nullable=false)
    private Long fileSize;

    @Column(name="checksum", nullable=false, length=64, unique=true)
    private String checksum;

    @Column(name="created_at", updatable=false, insertable=false)
    private LocalDateTime createdAt;
}
