package com.Team1_Back.client;

import com.Team1_Back.dto.LlmResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
public class OpenAiLlmClient {

    @Value("${openai.api.key}")
    private String apiKey;

    // ✅ chat/completions에서 안정적인 모델로 시작 추천
    @Value("${openai.model:gpt-4o-mini}")
    private String model;

    private static final String API_URL = "https://api.openai.com/v1/chat/completions";

    private final OkHttpClient client = new OkHttpClient.Builder()
            .callTimeout(Duration.ofSeconds(30))
            .build();

    private final ObjectMapper mapper = new ObjectMapper();

    public LlmResult chat(String prompt) {
        return ask(prompt);
    }

    public LlmResult ask(String prompt) {
        try {
            String bodyJson = """
            {
              "model": "%s",
              "temperature": 0.2,
              "messages": [
                {
                  "role": "system",
                  "content": "You are a helpful assistant that returns ONLY valid JSON. Do not wrap in markdown."
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
                    // ✅ 여기서 raw가 제일 중요함 (400/401/429 원인)
                    throw new RuntimeException("OpenAI API error: " + response.code() + " body=" + raw);
                }

                JsonNode root = mapper.readTree(raw);

                // ✅ NPE 방지: path(0) 사용
                JsonNode choice0 = root.path("choices").path(0);
                String text = choice0.path("message").path("content").asText(null);

                if (text == null) {
                    throw new RuntimeException("OpenAI response missing choices[0].message.content: " + raw);
                }

                return parseJson(text);
            }

        } catch (Exception e) {
            // ✅ 원인 메시지 유지
            throw new RuntimeException("OpenAI 호출 실패: " + e.getMessage(), e);
        }
    }

    private LlmResult parseJson(String text) throws Exception {
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
        JsonNode idArr = node.path("attachmentIds");

        // ✅ attachmentIds가 문자열/숫자 단일로 와도 처리
        if (idArr.isArray()) {
            for (JsonNode idNode : idArr) {
                if (idNode == null || idNode.isNull()) continue;
                if (idNode.isNumber()) ids.add(idNode.asLong());
                else {
                    String sId = idNode.asText("").trim();
                    if (!sId.isEmpty()) {
                        try { ids.add(Long.parseLong(sId)); } catch (NumberFormatException ignore) {}
                    }
                }
            }
        } else if (idArr.isNumber()) {
            ids.add(idArr.asLong());
        } else if (idArr.isTextual()) {
            String sId = idArr.asText("").trim();
            if (!sId.isEmpty()) {
                try { ids.add(Long.parseLong(sId)); } catch (NumberFormatException ignore) {}
            }
        }

        return new LlmResult(summary, List.of(), ids);
    }
}
