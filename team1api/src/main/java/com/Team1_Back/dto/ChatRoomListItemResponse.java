package com.Team1_Back.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class ChatRoomListItemResponse {
    private Long roomId;
    private String type;
    private String directKey;

    // ✅ 추가: 프론트에서 방 이름으로 사용
    private String partnerName;

    private Long lastMessageId;
    private Long lastSenderId;
    private String lastContent;
    private Instant lastCreatedAt;

    private long unreadCount;
}
