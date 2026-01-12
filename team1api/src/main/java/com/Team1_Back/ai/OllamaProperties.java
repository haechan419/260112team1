package com.Team1_Back.ai;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "ollama")
public class OllamaProperties {

    private String baseUrl;
    private String model;
    private Integer timeoutMs;
}
