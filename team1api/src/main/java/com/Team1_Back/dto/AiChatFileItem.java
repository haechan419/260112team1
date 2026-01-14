package com.Team1_Back.dto;


import java.time.LocalDateTime;

public record AiChatFileItem(
        Long attachmentId,
        Long roomId,
        Long messageId,
        String originalName,
        String fileUrl,
        LocalDateTime createdAt,
        String messageSnippet
) {}
