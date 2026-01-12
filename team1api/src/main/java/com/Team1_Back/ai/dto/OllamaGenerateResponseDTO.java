package com.Team1_Back.ai.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OllamaGenerateResponseDTO {
    private String response;
    private Boolean done;
    private String error;
}