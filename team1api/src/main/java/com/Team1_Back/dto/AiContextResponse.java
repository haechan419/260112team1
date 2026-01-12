package com.Team1_Back.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class AiContextResponse {
    private String summary;
    private List<AiContextMessageDto> messages;
}
