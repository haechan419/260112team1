package com.Team1_Back.client;

import com.Team1_Back.dto.LlmResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class OpenAiLlmClient {

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.model:gpt-4.1-nano}")
    private String model;

    private static final String API_URL = "https://api.openai.com/v1/chat/completions";

    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    public LlmResult ask(String prompt) {
        try {
            String bodyJson = """
            {
              "model": "%s",
              "temperature": 0.2,
              "messages": [
                {
                  "role": "system",
                  "content": "You are a helpful assistant that returns ONLY valid JSON."
                },
                {
                  "role": "user",
                  "content": %s
                }
              ]
            }
            """.formatted(
                    model,
                    mapper.writeValueAsString(prompt)
            );

            Request request = new Request.Builder()
                    .url(API_URL)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(bodyJson, MediaType.parse("application/json")))
                    .build();

            try (Response response = client.newCall(request).execute()) {
                String raw = response.body() != null ? response.body().string() : "";

                if (!response.isSuccessful()) {
                    throw new RuntimeException("OpenAI API error: " + response.code() + " " + raw);
                }

                String text = mapper.readTree(raw)
                        .path("choices").get(0)
                        .path("message")
                        .path("content")
                        .asText();

                return parseJson(text);
            }

        } catch (Exception e) {
            throw new RuntimeException("OpenAI 호출 실패", e);
        }
    }

    private LlmResult parseJson(String text) throws Exception {
        // ```json 제거
        String cleaned = text
                .replaceAll("(?s)```json\\s*", "")
                .replaceAll("(?s)```\\s*", "")
                .trim();

        int s = cleaned.indexOf('{');
        int e = cleaned.lastIndexOf('}');
        if (s >= 0 && e > s) cleaned = cleaned.substring(s, e + 1);

        JsonNode node = mapper.readTree(cleaned);

        String summary = node.path("summary").asText("");
        List<Long> ids = new ArrayList<>();
        for (JsonNode idNode : node.path("messageIds")) {
            ids.add(idNode.asLong());
        }

        return new LlmResult(summary, ids);
    }
}
