package com.Team1_Back.ai;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * ✅ 새로 생성: 영수증 OCR 통합 - Python AI 서비스 RestClient 설정
 * Python AI 서비스 RestClient 설정
 * 
 * @author Team1
 */
@Configuration
@EnableConfigurationProperties(ReceiptAiProperties.class)
public class ReceiptAiConfig {

    @Bean
    public RestClient receiptAiRestClient(ReceiptAiProperties props) {
        return RestClient.builder()
                .baseUrl(props.getBaseUrl())
                .requestInterceptor((request, body, execution) -> {
                    // 타임아웃 설정이 필요하면 여기서 추가
                    return execution.execute(request, body);
                })
                .build();
    }
}

