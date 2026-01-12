package com.Team1_Back.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReceiptDTO {

    private Long id;

    private Long expenseId;

    private Long uploadedBy;

    private String uploadedByName;

    private String fileUrl;

    private String fileHash;

    private String mimeType;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // AI 추출 결과
    private Long extractionId;

    private String modelName;

    private String extractedJson;

    private LocalDate extractedDate;

    private Integer extractedAmount;

    private String extractedMerchant;

    private String extractedCategory;

    private String extractedDescription;

    private BigDecimal confidence;

    private LocalDateTime extractionCreatedAt;

    // 검증 결과
    private Long verificationId;

    private Long verifiedBy;

    private String verifiedByName;

    private String verifiedMerchant;

    private Integer verifiedAmount;

    private String verifiedCategory;

    private String reason;

    private LocalDateTime verificationCreatedAt;

    // 지출 상태 (Expense의 상태)
    private String status; // DRAFT, SUBMITTED, APPROVED, REJECTED, REQUEST_MORE_INFO
}

