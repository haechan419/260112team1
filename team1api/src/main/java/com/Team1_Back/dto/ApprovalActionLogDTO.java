package com.Team1_Back.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ApprovalActionLogDTO {

    private Long id;

    private Long approvalRequestId;

    private Long actorId;

    private String actorName;

    private String action; // SUBMIT, APPROVE, REJECT, REQUEST_MORE_INFO, COMMENT

    private String message;

    private LocalDateTime createdAt;
}

