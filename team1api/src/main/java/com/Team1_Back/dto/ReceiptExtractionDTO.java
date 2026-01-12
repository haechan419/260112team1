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
public class ReceiptExtractionDTO {

    private Long receiptId;

    private String modelName;

    private LocalDate extractedDate;

    private Integer extractedAmount;

    private String extractedMerchant;

    private String extractedCategory;

    private String extractedDescription;

    private BigDecimal confidence;

    private String extractedJson;

    private LocalDateTime createdAt;
}

