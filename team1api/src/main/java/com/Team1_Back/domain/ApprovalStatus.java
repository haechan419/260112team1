package com.Team1_Back.domain;

public enum ApprovalStatus {
    DRAFT,           // 작성 중 (임시저장)
    SUBMITTED,       // 결재 상신 (승인 대기)
    APPROVED,        // 승인됨 (최종 완료)
    REJECTED,        // 반려됨 (거절)
    REQUEST_MORE_INFO // 보완 요청
}

