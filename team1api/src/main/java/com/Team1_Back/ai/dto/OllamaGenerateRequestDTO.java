package com.Team1_Back.ai.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OllamaGenerateRequestDTO {
    private String model;
    private String prompt;
    private boolean stream = false;
}