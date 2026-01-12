package com.Team1_Back.dto;

// com.Team1_Back.dto.ChatMessageBroadcastDto


import lombok.*;

import java.time.Instant;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ChatMessageBroadcastDto {
    private String type; // "MESSAGE"
    private Long roomId;
    private Long messageId;
    private Long senderId;
    private String content;      // "" 가능
    private Instant createdAt;
    private List<ChatAttachmentDto> attachments;
}

