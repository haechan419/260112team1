package com.Team1_Back.dto;


import java.time.LocalDateTime;

public record ChatAttachmentSearchRow(
        Long attachmentId,
        Long roomId,
        Long messageId,
        Long uploaderId,
        String originalName,
        String mimeType,
        Long fileSize,
        String fileUrl,
        LocalDateTime createdAt,
        String messageSnippet
) {}