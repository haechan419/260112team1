package com.Team1_Back.domain;

import jakarta.persistence.*;
import lombok.*;
// ✅ 제거: JdbcTypeCode import (JSON 타입 사용 안 함)
// import org.hibernate.annotations.JdbcTypeCode;
// import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "receipt_ai_extraction", indexes = {
    @Index(name = "idx_receipt_id", columnList = "receipt_id"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"receipt"})
public class ReceiptAiExtraction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receipt_id", nullable = false, unique = true)
    private ReceiptUpload receipt;

    @Column(name = "model_name", length = 50, nullable = false)
    private String modelName; // gpt-4o-vision 등

//    @JdbcTypeCode(SqlTypes.JSON)
//    @Column(name = "extracted_json", nullable = false, columnDefinition = "JSON")
    // 유진님 추가
    // ✅ 변경: JSON 타입에서 TEXT 타입으로 변경 (영수증 원문 텍스트는 유효한 JSON 형식이 아니므로)
    @Column(name = "extracted_json", nullable = false, columnDefinition = "TEXT")
    private String extractedJson; // 원문 추출 결과(raw)

    @Column(name = "extracted_date")
    private LocalDate extractedDate;

    @Column(name = "extracted_amount")
    private Integer extractedAmount;

    @Column(name = "extracted_merchant", length = 150)
    private String extractedMerchant;

    @Column(name = "extracted_category", length = 50)
    private String extractedCategory;

    @Column(name = "extracted_description", columnDefinition = "TEXT")
    private String extractedDescription;

    @Column(name = "confidence", precision = 5, scale = 2)
    private BigDecimal confidence;
}

