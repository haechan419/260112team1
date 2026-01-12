package com.Team1_Back.ai.dto;
//요청 dto
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiGenerateRequestDTO {
    private String prompt;
}