//package com.Team1_Back.client;
//
//import com.Team1_Back.dto.LlmResult;
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import okhttp3.*;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Component;
//
//import java.util.ArrayList;
//import java.util.List;
//
//@Component
//public class GeminiLlmClient {
//
//    @Value("${gemini.api.key:}")
//    private String apiKey;
//
//    // ✅ 풀네임 그대로 받는다: "models/gemini-2.5-flash"
//    @Value("${gemini.model:models/gemini-2.5-flash}")
//    private String model;
//
//    private final OkHttpClient client = new OkHttpClient();
//    private final ObjectMapper mapper = new ObjectMapper();
//
//    private String apiUrl() {
//        // ✅ v1beta + model 풀네임 그대로
//        return "https://generativelanguage.googleapis.com/v1beta/" + model + ":generateContent?key=" + apiKey;
//    }
//
//    public LlmResult ask(String prompt) {
//        try {
//            if (apiKey == null || apiKey.isBlank()) {
//                throw new RuntimeException("Gemini API key is missing");
//            }
//
//            String bodyJson = """
//            {
//              "contents": [
//                {
//                  "parts": [
//                    { "text": %s }
//                  ]
//                }
//              ]
//            }
//            """.formatted(mapper.writeValueAsString(prompt));
//
//            Request request = new Request.Builder()
//                    .url(apiUrl())
//                    .addHeader("Content-Type", "application/json")
//                    .post(RequestBody.create(bodyJson, MediaType.parse("application/json")))
//                    .build();
//
//            try (Response response = client.newCall(request).execute()) {
//                String raw = response.body() != null ? response.body().string() : "";
//
//                if (!response.isSuccessful()) {
//                    throw new RuntimeException("Gemini API error: " + response.code() + " " + raw);
//                }
//
//                String text = mapper.readTree(raw)
//                        .path("candidates").get(0)
//                        .path("content")
//                        .path("parts").get(0)
//                        .path("text")
//                        .asText();
//
//                return parseJsonOrThrow(text);
//            }
//
//        } catch (Exception e) {
//            throw new RuntimeException("Gemini 호출 실패", e);
//        }
//    }
//
//    private LlmResult parseJsonOrThrow(String maybeJson) throws Exception {
//        // ✅ ```json ... ``` 제거
//        String cleaned = maybeJson
//                .replaceAll("(?s)```json\\s*", "")
//                .replaceAll("(?s)```\\s*", "")
//                .trim();
//
//        // ✅ 혹시 JSON 앞뒤로 텍스트 섞이면, 첫 { ~ 마지막 }만 잘라서 파싱
//        int s = cleaned.indexOf('{');
//        int e = cleaned.lastIndexOf('}');
//        if (s >= 0 && e > s) cleaned = cleaned.substring(s, e + 1);
//
//        JsonNode node = mapper.readTree(cleaned);
//
//        String summary = node.path("summary").asText("");
//        List<Long> ids = new ArrayList<>();
//        for (JsonNode idNode : node.path("messageIds")) {
//            ids.add(idNode.asLong());
//        }
//        return new LlmResult(summary, ids);
//    }
//}
