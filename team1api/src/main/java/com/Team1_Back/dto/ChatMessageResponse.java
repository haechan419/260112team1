package com.Team1_Back.dto;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class ChatMessageResponse {
    private Long messageId;
    private Long roomId;
    private Long senderId;
    private String content;
    private Instant createdAt;
}
