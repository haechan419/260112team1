package com.Team1_Back.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ApprovalActionDTO {

    private String action; // APPROVE, REJECT, REQUEST_MORE_INFO

    private String message; // 반려 사유 또는 보완 요청 사유
}

