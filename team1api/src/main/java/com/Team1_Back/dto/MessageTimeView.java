package com.Team1_Back.dto;

import java.time.Instant;

public record MessageTimeView(
        Long messageId,
        Long roomId,
        Instant createdAt
) {}
