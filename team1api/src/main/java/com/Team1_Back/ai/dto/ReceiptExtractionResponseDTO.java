package com.Team1_Back.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * ✅ 새로 생성: 영수증 OCR 통합 - Python AI 서비스로부터 받는 OCR 추출 결과 응답 DTO
 * Python AI 서비스로부터 받는 OCR 추출 결과 응답 DTO
 * 
 * @author Team1
 */
@Data
public class ReceiptExtractionResponseDTO {

    @JsonProperty("extractedMerchant")
    private String extractedMerchant;

    @JsonProperty("extractedAmount")
    private Integer extractedAmount;

    @JsonProperty("extractedDate")
    private String extractedDate; // "YYYY-MM-DD" 형식의 문자열

    @JsonProperty("extractedCategory")
    private String extractedCategory;

    @JsonProperty("extractedDescription")
    private String extractedDescription;

    @JsonProperty("confidence")
    private BigDecimal confidence;

    @JsonProperty("extractedJson")
    private String extractedJson;

    @JsonProperty("modelName")
    private String modelName;

    @JsonProperty("error")
    private String error; // 에러 발생 시 메시지
}

