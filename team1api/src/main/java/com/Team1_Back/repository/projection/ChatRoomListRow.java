package com.Team1_Back.repository.projection;

import java.time.LocalDateTime;

public interface ChatRoomListRow {
    Long getRoomId();
    String getType();
    String getDirectKey();

    // ✅ 추가
    String getPartnerName();

    Long getLastMessageId();
    Long getLastSenderId();
    String getLastContent();
    LocalDateTime getLastCreatedAt();

    Long getUnreadCount();
}

