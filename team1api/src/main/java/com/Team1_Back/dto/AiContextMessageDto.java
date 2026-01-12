package com.Team1_Back.dto;

import lombok.Getter;

import java.time.Instant;

@Getter
public class AiContextMessageDto {

    private Long messageId;
    private String content;
    private Instant  createdAt;
    private Long roomId;

    public AiContextMessageDto(
            Long messageId,
            String content,
            Instant createdAt,
            Long roomId
    ) {
        this.messageId = messageId;
        this.content = content;
        this.createdAt = createdAt;
        this.roomId = roomId;
    }
}

