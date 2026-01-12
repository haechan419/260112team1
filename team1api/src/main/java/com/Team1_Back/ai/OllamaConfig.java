package com.Team1_Back.ai;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(OllamaProperties.class)
public class OllamaConfig {

    @Bean
    public RestClient ollamaRestClient(OllamaProperties props) {
        // 타임아웃 설정 (기본값: 60초, 설정값이 있으면 사용)
        int timeoutMs = props.getTimeoutMs() != null ? props.getTimeoutMs() : 60000;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(5000)); // 연결 타임아웃: 5초
        factory.setReadTimeout(Duration.ofMillis(timeoutMs)); // 읽기 타임아웃: 설정값 사용

        return RestClient.builder()
                .baseUrl(props.getBaseUrl())
                .requestFactory(factory)
                .build();
    }
}
