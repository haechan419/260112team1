package com.Team1_Back.domain;

import jakarta.persistence.*;
import lombok.*;

/**
 * 영수증 업로드 엔티티
 * 
 * <p>사용자가 업로드한 영수증 이미지 정보를 관리합니다.
 * 지출 내역과 1:1 관계이며, 파일 URL, 해시, MIME 타입 등의 정보를 저장합니다.
 * 
 * @author Team1
 */
@Entity
@Table(name = "receipt_upload", indexes = {
    @Index(name = "idx_expense_id", columnList = "expense_id"),
    @Index(name = "idx_file_hash", columnList = "file_hash")
})
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"expense", "uploadedBy"})
public class ReceiptUpload extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 연결된 지출 내역 (1:1 관계) */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_id", nullable = false, unique = true)
    private Expense expense;

    /** 업로드한 사용자 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;

    /** 영수증 이미지 파일 URL */
    @Column(name = "file_url", length = 255, nullable = false)
    private String fileUrl;

    /** 파일 해시값 (중복 검사용) */
    @Column(name = "file_hash", length = 64)
    private String fileHash;

    /** 파일 MIME 타입 (예: image/jpeg) */
    @Column(name = "mime_type", length = 50)
    private String mimeType;
}

