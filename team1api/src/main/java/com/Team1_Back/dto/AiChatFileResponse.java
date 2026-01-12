package com.Team1_Back.dto;

import java.util.List;

public record AiChatFileResponse(
        String summary,
        List<AiChatFileItem> files
) {
    public static AiChatFileResponse empty() {
        return new AiChatFileResponse(
                "관련된 파일을 찾지 못했습니다.",
                List.of()
        );
    }

    // ✅ 이거 추가하면 끝
    public static AiChatFileResponse empty(String summary) {
        return new AiChatFileResponse(
                summary,
                List.of()
        );
    }
}
