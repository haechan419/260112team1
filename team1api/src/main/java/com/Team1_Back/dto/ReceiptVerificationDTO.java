package com.Team1_Back.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReceiptVerificationDTO {

    private String action; // APPROVE, REJECT, REQUEST_MORE_INFO

    private Long expenseId; // 영수증이 없는 경우 사용

    private String verifiedMerchant;

    private Integer verifiedAmount;

    private String verifiedCategory;

    private String reason;
}

