package com.Team1_Back.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.time.LocalDateTime; // 날짜 타입 추가

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RequestDTO {

    private Long rno; // 요청 고유 번호 (PK)
    private String status; // 진행 상태 (PENDING, APPROVED...)
    private LocalDateTime regDate; // 등록일

    private String requester; // 요청자
    private String reason; // 사유
    private String rejectReason; // 반려사유
    private int totalAmount; // 총 합계

    private List<RequestItemDTO> items; // 품목 리스트
}