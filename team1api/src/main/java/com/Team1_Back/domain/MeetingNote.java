package com.Team1_Back.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "meeting_note")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeetingNote extends BaseEntity {
    // 고유 번호
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 업로드한 사용자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 원본 파일명
    @Column(name = "original_file_name", nullable = false)
    private String originalFileName;

    // 저장된 파일명
    @Column(name = "file_name", nullable = false)
    private String fileName;

    // 파일 경로
    @Column(name = "file_path", nullable = false)
    private String filePath;

    // 파일 MIME 타입
    @Column(name = "file_type", nullable = false)
    private String fileType;

    // 파일 크기
    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    // AI 분석 완료 여부
    @Column(name = "analyzed", nullable = false)
    @Builder.Default
    private Boolean analyzed = false;

    // 업로드 일시
    @Column(name = "upload_date", nullable = false)
    private LocalDateTime uploadDate;

    // 분석 완료 여부 확인
    public boolean isAnalyzed() {
        return this.analyzed != null && this.analyzed;
    }
}
