package com.Team1_Back.ai.service;

import com.Team1_Back.ai.OllamaProperties;
import com.Team1_Back.ai.dto.OllamaGenerateRequestDTO;
import com.Team1_Back.ai.dto.OllamaGenerateResponseDTO;
import com.Team1_Back.dto.TodoDTO;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiServiceImpl implements AiService {

    private final RestClient ollamaRestClient;
    private final OllamaProperties props;
    private final Gson gson = new Gson();

    @Override
    public String generate(String prompt) {

        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("prompt is blank");
        }

        OllamaGenerateRequestDTO req = new OllamaGenerateRequestDTO();
        req.setModel(props.getModel());
        req.setPrompt(prompt);
        req.setStream(false); // ★ 중요

        try {
            OllamaGenerateResponseDTO res = ollamaRestClient.post()
                    .uri("/api/generate")
                    .body(req)
                    .retrieve()
                    .body(OllamaGenerateResponseDTO.class);

            if (res == null) {
                throw new RuntimeException("ollama response is null");
            }
            if (res.getError() != null && !res.getError().isBlank()) {
                throw new RuntimeException("ollama error: " + res.getError());
            }
            if (res.getResponse() == null) {
                throw new RuntimeException("ollama response.text is null");
            }

            return res.getResponse();

        } catch (Exception e) {
            log.error("[AI] Ollama call failed", e);
            throw new RuntimeException("AI generate failed", e);
        }
    }

    @Override
    public List<TodoDTO> analyzeMeetingNote(String fileContent) {
        if (fileContent == null || fileContent.isBlank()) {
            throw new IllegalArgumentException("회의록 내용이 비어있습니다.");
        }

        // AI 프롬프트 생성
        String prompt = createMeetingNoteAnalysisPrompt(fileContent);

        log.info("[AI] 회의록 분석 시작");

        try {
            // AI 호출
            String aiResponse = generate(prompt);
            log.info("[AI] AI 응답 받음: {}", aiResponse.substring(0, Math.min(200, aiResponse.length())));

            // JSON 파싱
            List<TodoDTO> todos = parseTodoListFromJson(aiResponse);
            log.info("[AI] Todo 추출 완료: {}개", todos.size());

            return todos;

        } catch (Exception e) {
            log.error("[AI] 회의록 분석 실패", e);
            throw new RuntimeException("회의록 분석에 실패했습니다: " + e.getMessage(), e);
        }
    }

    // 회의록 분석을 위한 프롬프트를 생성
    private String createMeetingNoteAnalysisPrompt(String fileContent) {
        LocalDate today = LocalDate.now();
        String todayStr = today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        // 회의록에서 날짜 추출 시도 (예: "2026년 01월 06일", "2026-01-06" 등)
        String meetingDate = extractMeetingDate(fileContent);

        return """
                다음 회의록 내용을 분석하여 할 일(Todo) 목록을 추출해주세요.

                현재 날짜: %s
                회의록 날짜: %s

                회의록 내용:
                %s

                다음 JSON 형식으로 응답해주세요:
                {
                  "todos": [
                    {
                      "title": "할 일 제목",
                      "content": "할 일 상세 설명",
                      "dueDate": "YYYY-MM-DD 형식의 마감일 (없으면 null)",
                      "priority": "HIGH, MEDIUM, LOW 중 하나",
                      "status": "TODO"
                    }
                  ]
                }

                중요: 날짜 처리 규칙
                1. 회의록 날짜가 "%s"로 명시되어 있습니다. 이 날짜를 모든 할 일의 기본 마감일로 반드시 사용하세요.
                2. 각 할 일에 별도의 마감일이 명시되지 않았다면, 반드시 "%s"를 마감일로 설정하세요.
                3. "다음 주", "내일" 같은 상대적 표현이 있으면 현재 날짜(%s)를 기준으로 계산하되, 계산 결과가 회의록 날짜보다 이전이면 회의록 날짜를 사용하세요.
                4. 특정 할 일에 명확한 다른 날짜가 명시된 경우에만 그 날짜를 사용하세요.
                5. 날짜가 전혀 명시되지 않았으면 "%s"를 사용하세요.

                주의사항:
                1. 할 일이 명확하지 않으면 포함하지 마세요.
                2. 마감일은 반드시 YYYY-MM-DD 형식으로만 작성하세요 (예: 2026-01-06).
                3. 우선순위가 명시되지 않았으면 MEDIUM으로 설정하세요.
                4. 반드시 유효한 JSON 형식으로만 응답하세요.
                5. 다른 설명이나 텍스트는 포함하지 마세요.
                """.formatted(todayStr, meetingDate != null ? meetingDate : "미확인", fileContent,
                meetingDate != null ? meetingDate : "회의록의 날짜",
                meetingDate != null ? meetingDate : "회의록의 날짜",
                todayStr,
                meetingDate != null ? meetingDate : "회의록의 날짜");
    }

    // 회의록 내용에서 날짜 추출
    private String extractMeetingDate(String fileContent) {
        if (fileContent == null || fileContent.isBlank()) {
            return null;
        }

        // 다양한 날짜 형식 패턴 매칭
        // "2026년 01월 06일", "2026-01-06", "2026/01/06" 등
        java.util.regex.Pattern pattern1 = java.util.regex.Pattern.compile(
                "(\\d{4})[년\\s-/.]+(\\d{1,2})[월\\s-/.]+(\\d{1,2})[일]?");
        java.util.regex.Matcher matcher = pattern1.matcher(fileContent);

        if (matcher.find()) {
            try {
                int year = Integer.parseInt(matcher.group(1));
                int month = Integer.parseInt(matcher.group(2));
                int day = Integer.parseInt(matcher.group(3));

                // 유효한 날짜인지 확인
                LocalDate date = LocalDate.of(year, month, day);
                return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            } catch (Exception e) {
                log.warn("[AI] 날짜 추출 실패: {}", e.getMessage());
            }
        }

        return null;
    }

    // AI 응답에서 Todo 목록을 파싱
    private List<TodoDTO> parseTodoListFromJson(String aiResponse) {
        List<TodoDTO> todos = new ArrayList<>();

        try {
            // JSON 응답에서 JSON 부분만 추출 (마크다운 코드 블록 제거)
            String jsonStr = extractJsonFromResponse(aiResponse);

            JsonObject jsonObject = gson.fromJson(jsonStr, JsonObject.class);
            JsonArray todosArray = jsonObject.getAsJsonArray("todos");

            if (todosArray == null) {
                log.warn("[AI] 'todos' 배열을 찾을 수 없습니다.");
                return todos;
            }

            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            for (JsonElement element : todosArray) {
                JsonObject todoObj = element.getAsJsonObject();

                TodoDTO todo = TodoDTO.builder()
                        .title(getStringValue(todoObj, "title", ""))
                        .content(getStringValue(todoObj, "content", ""))
                        .dueDate(parseDate(getStringValue(todoObj, "dueDate", null), dateFormatter))
                        .priority(getStringValue(todoObj, "priority", "MEDIUM").toUpperCase())
                        .status(getStringValue(todoObj, "status", "TODO").toUpperCase())
                        .build();

                // 유효성 검증
                if (todo.getTitle() != null && !todo.getTitle().isBlank()) {
                    todos.add(todo);
                }
            }

        } catch (Exception e) {
            log.error("[AI] JSON 파싱 실패: {}", e.getMessage());
            throw new RuntimeException("AI 응답을 파싱할 수 없습니다: " + e.getMessage(), e);
        }

        return todos;
    }

    // 응답에서 JSON 부분만 추출합니다 (마크다운 코드 블록 제거)
    private String extractJsonFromResponse(String response) {
        String trimmed = response.trim();

        // 마크다운 코드 블록 제거
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }

        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }

        return trimmed.trim();
    }

    // JsonObject에서 문자열 값을 가져옴
    private String getStringValue(JsonObject obj, String key, String defaultValue) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) {
            return defaultValue;
        }
        return obj.get(key).getAsString();
    }

    // 날짜 문자열을 LocalDate로 파싱
    private LocalDate parseDate(String dateStr, DateTimeFormatter formatter) {
        if (dateStr == null || dateStr.isBlank() || dateStr.equalsIgnoreCase("null")) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr, formatter);
        } catch (Exception e) {
            log.warn("[AI] 날짜 파싱 실패: {}", dateStr);
            return null;
        }
    }
}
