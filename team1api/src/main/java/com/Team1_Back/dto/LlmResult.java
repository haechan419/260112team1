package com.Team1_Back.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class LlmResult {
    private String summary;
    private List<Long> messageIds;
}

