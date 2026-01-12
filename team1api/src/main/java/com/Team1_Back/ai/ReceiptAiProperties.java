package com.Team1_Back.ai;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * ✅ 새로 생성: 영수증 OCR 통합 - Python AI 서비스 설정 프로퍼티
 * Python AI 서비스 설정 프로퍼티
 * 
 * @author Team1
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "receipt.ai")
public class ReceiptAiProperties {

    /**
     * Python AI 서비스의 기본 URL
     * 예: http://localhost:8000
     */
    private String baseUrl;

    /**
     * 요청 타임아웃 (밀리초)
     * 기본값: 60000 (60초)
     */
    private Integer timeoutMs = 60000;
}

