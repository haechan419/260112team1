package com.Team1_Back.ai;
import com.Team1_Back.client.OpenAiLlmClient;
import com.Team1_Back.dto.LlmResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LlmRouter {

    private final OpenAiLlmClient openai;

    public LlmResult ask(String prompt) {
        return openai.ask(prompt);
    }
}