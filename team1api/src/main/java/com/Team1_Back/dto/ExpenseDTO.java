package com.Team1_Back.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExpenseDTO {

    private Long id;

    private Long userId;

    private String userName;

    private LocalDate receiptDate;

    private String merchant;

    private Integer amount;

    private String category;

    private String status; // DRAFT, SUBMITTED, APPROVED, REJECTED, NEED_MORE_INFO

    private String description;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // 영수증 관련 정보
    private Long receiptId;

    private String receiptFileUrl;

    private String receiptImageUrl;

    private Boolean hasReceipt;
}

