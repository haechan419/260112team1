package com.Team1_Back.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class LlmResult {
    private String summary;
    private List<Long> messageIds;      // 기존 컨텍스트
    private List<Long> attachmentIds;   // ✅ 파일 선택용 추가
}
