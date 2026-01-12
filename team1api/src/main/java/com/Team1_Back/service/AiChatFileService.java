package com.Team1_Back.service;

import com.Team1_Back.ai.LlmRouter;
import com.Team1_Back.dto.*;
import com.Team1_Back.repository.ChatMessageRepository;
import com.Team1_Back.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiChatFileService {

    private final ChatAttachmentSearchService chatAttachmentSearchService; // ✅ 글로벌 후보 검색
    private final ChatAttachmentQueryService attachmentQueryService;       // ✅ direct + nearby 후보 조회
    private final ChatMessageRepository messageRepo;                       // ✅ messageIds -> (roomId, createdAt Instant)
    private final LlmRouter llmRouter;

    /**
     * ✅ 신규: 채팅방 안 열어도 "내 전체 채팅"에서 관련 파일 찾기
     * - DB 후보 검색(파일명/메시지) → LLM이 attachmentIds 선택 → 후보 밖 제거(환각 방지)
     */
    public AiChatFileResponse findChatFilesGlobal(String query) {

        Long me = SecurityUtil.currentUserId();
        if (me == null) throw new AccessDeniedException("UNAUTHORIZED");

        if (query == null || query.trim().isEmpty()) {
            return AiChatFileResponse.empty("query is required");
        }

        String likeQ = toLikePattern(query);
        log.info("[AI-FILE-G] me={} query='{}' likeQ='{}'", me, query, likeQ);

        List<ChatAttachmentSearchRow> candidates =
                chatAttachmentSearchService.searchGlobal(me, likeQ, 30, 0);

        log.info("[AI-FILE-G] candidates.size={}", (candidates == null ? -1 : candidates.size()));

        if (candidates == null || candidates.isEmpty()) {
            return AiChatFileResponse.empty("관련된 파일을 찾지 못했습니다.");
        }

        // ✅ candidates(ChatAttachmentSearchRow)용 LLM 선별
        return pickBestFilesFromSearchRows(query, candidates);
    }

    /**
     * ✅ 1번 목표: "AI가 고른 messageIds" 기반으로
     *  - messageIds에 직접 첨부된 파일 후보를 DB에서 가져오고
     *  - (direct 없으면) 같은 roomId에서 createdAt ± N분 범위 첨부까지 후보 확장
     *  - LLM이 그 중에서 최적 파일을 선택
     */
    public AiChatFileResponse findFilesByContext(String userQuery, List<Long> messageIds) {

        Long me = SecurityUtil.currentUserId();
        if (me == null) throw new AccessDeniedException("UNAUTHORIZED");

        if (userQuery == null || userQuery.trim().isEmpty()) {
            return AiChatFileResponse.empty("query is required");
        }

        if (messageIds == null || messageIds.isEmpty()) {
            return AiChatFileResponse.empty("messageIds is required");
        }

        // =========================================================
        // 1️⃣ 메시지에 직접 달린 파일 후보
        // =========================================================
        List<AiChatFileItem> directCandidates =
                attachmentQueryService.findDirectAttachmentsByMessages(messageIds);

        log.info("[AI-FILE-C] messageIds={} directCandidates={}",
                messageIds.size(), (directCandidates == null ? -1 : directCandidates.size()));

        if (directCandidates != null && !directCandidates.isEmpty()) {
            return pickBestFilesFromItems(userQuery, directCandidates);
        }

        // =========================================================
        // 2️⃣ fallback: 같은 roomId에서 createdAt ± N분 범위 첨부 후보 확장
        // =========================================================
        int nearbyMinutes = 7;          // ✅ 시작값 추천: 5~10
        int maxFallbackCandidates = 40; // ✅ 폭주 방지

        List<MessageTimeView> times = messageRepo.findMessageTimes(messageIds);

        if (times == null || times.isEmpty()) {
            return AiChatFileResponse.empty("선택된 대화의 시간 정보를 찾지 못했습니다.");
        }

        Map<Long, List<MessageTimeView>> byRoom = times.stream()
                .filter(t -> t != null && t.roomId() != null && t.createdAt() != null)
                .collect(Collectors.groupingBy(MessageTimeView::roomId));

        LinkedHashMap<Long, AiChatFileItem> merged = new LinkedHashMap<>();
        ZoneId zone = ZoneId.systemDefault();

        for (Map.Entry<Long, List<MessageTimeView>> e : byRoom.entrySet()) {
            Long roomId = e.getKey();
            List<MessageTimeView> list = e.getValue();

            Instant min = list.stream().map(MessageTimeView::createdAt).min(Instant::compareTo).orElse(null);
            Instant max = list.stream().map(MessageTimeView::createdAt).max(Instant::compareTo).orElse(null);
            if (min == null || max == null) continue;

            Instant fromI = min.minus(Duration.ofMinutes(nearbyMinutes));
            Instant toI   = max.plus(Duration.ofMinutes(nearbyMinutes));

            LocalDateTime from = LocalDateTime.ofInstant(fromI, zone);
            LocalDateTime to   = LocalDateTime.ofInstant(toI, zone);

            List<AiChatFileItem> near =
                    attachmentQueryService.findNearbyAttachments(roomId, from, to);

            if (near == null || near.isEmpty()) continue;

            for (AiChatFileItem item : near) {
                if (item == null || item.attachmentId() == null) continue;
                merged.putIfAbsent(item.attachmentId(), item);
                if (merged.size() >= maxFallbackCandidates) break;
            }
            if (merged.size() >= maxFallbackCandidates) break;
        }

        List<AiChatFileItem> fallbackCandidates = new ArrayList<>(merged.values());

        log.info("[AI-FILE-C] fallbackCandidates={} (nearby ±{}m)", fallbackCandidates.size(), nearbyMinutes);

        if (fallbackCandidates.isEmpty()) {
            return AiChatFileResponse.empty("선택된 대화 메시지에 직접 첨부된 파일이 없고, 근처 업로드 파일도 없습니다.");
        }

        // 3️⃣ AI에게 어떤 파일이 핵심인지 판단
        return pickBestFilesFromItems(userQuery, fallbackCandidates);
    }

    // =========================================================
    // ✅ LLM 선별 로직 (공통)
    // =========================================================

    /**
     * candidates가 ChatAttachmentSearchRow(글로벌 검색 후보)일 때 사용
     */
    private AiChatFileResponse pickBestFilesFromSearchRows(String userQuery, List<ChatAttachmentSearchRow> candidates) {

        // ✅ (중요) attachmentId null row 제거 -> toMap NPE 방지
        List<ChatAttachmentSearchRow> valid = candidates.stream()
                .filter(Objects::nonNull)
                .filter(r -> r.attachmentId() != null)
                .toList();

        if (valid.isEmpty()) {
            return AiChatFileResponse.empty("검색 후보에 유효한 attachmentId가 없습니다.");
        }

        // ✅ LLM 폭주 방지 (필요 시 조절)
        int maxForLlm = 60;
        if (valid.size() > maxForLlm) valid = valid.subList(0, maxForLlm);

        String prompt = buildPromptForFiles(valid, userQuery);

        LlmResult llmResult;
        try {
            llmResult = llmRouter.ask(prompt);
        } catch (Exception ex) {
            log.warn("[AI-FILE-G] LLM failed -> fallback. {}", ex.toString());
            // fallback: 상위 5개 반환
            List<AiChatFileItem> fallback = valid.stream().limit(5).map(r ->
                    new AiChatFileItem(
                            r.attachmentId(),
                            r.roomId(),
                            r.messageId(),
                            r.originalName(),
                            r.fileUrl(),
                            r.createdAt(),
                            r.messageSnippet()
                    )
            ).toList();
            return new AiChatFileResponse("AI 호출 실패로 검색 결과 상위 파일을 반환합니다.", fallback);
        }

        Set<Long> allowed = valid.stream()
                .map(ChatAttachmentSearchRow::attachmentId)
                .collect(Collectors.toSet());

        List<Long> picked = Optional.ofNullable(llmResult.getAttachmentIds())
                .orElse(List.of())
                .stream()
                .filter(Objects::nonNull)
                .filter(allowed::contains)
                .distinct()
                .limit(5)
                .toList();

        Map<Long, ChatAttachmentSearchRow> map = valid.stream()
                .collect(Collectors.toMap(ChatAttachmentSearchRow::attachmentId, r -> r, (a, b) -> a));

        List<AiChatFileItem> files = picked.stream()
                .map(id -> {
                    ChatAttachmentSearchRow r = map.get(id);
                    if (r == null) return null;
                    return new AiChatFileItem(
                            r.attachmentId(),
                            r.roomId(),
                            r.messageId(),
                            r.originalName(),
                            r.fileUrl(),
                            r.createdAt(),
                            r.messageSnippet()
                    );
                })
                .filter(Objects::nonNull)
                .toList();

        String summary = (llmResult.getSummary() == null || llmResult.getSummary().isBlank())
                ? "관련 파일 후보 중 상위 결과입니다."
                : llmResult.getSummary();

        return new AiChatFileResponse(summary, files);
    }

    /**
     * candidates가 AiChatFileItem(메시지 직접첨부/nearby 확장 후보)일 때 사용
     */
    private AiChatFileResponse pickBestFilesFromItems(String userQuery, List<AiChatFileItem> candidates) {

        List<AiChatFileItem> valid = candidates.stream()
                .filter(Objects::nonNull)
                .filter(r -> r.attachmentId() != null)
                .toList();

        if (valid.isEmpty()) {
            return AiChatFileResponse.empty("유효한 첨부파일 후보가 없습니다.");
        }

        int maxForLlm = 60;
        if (valid.size() > maxForLlm) valid = valid.subList(0, maxForLlm);

        String prompt = buildPromptForFileItems(valid, userQuery);

        LlmResult llmResult;
        try {
            llmResult = llmRouter.ask(prompt);
        } catch (Exception ex) {
            log.warn("[AI-FILE-C] LLM failed -> fallback. {}", ex.toString());
            List<AiChatFileItem> fallback = valid.stream().limit(5).toList();
            return new AiChatFileResponse("AI 호출 실패로 후보 상위 파일을 반환합니다.", fallback);
        }

        Set<Long> allowed = valid.stream()
                .map(AiChatFileItem::attachmentId)
                .collect(Collectors.toSet());

        List<Long> picked = Optional.ofNullable(llmResult.getAttachmentIds())
                .orElse(List.of())
                .stream()
                .filter(Objects::nonNull)
                .filter(allowed::contains)
                .distinct()
                .limit(5)
                .toList();

        Map<Long, AiChatFileItem> map = valid.stream()
                .collect(Collectors.toMap(AiChatFileItem::attachmentId, r -> r, (a, b) -> a));

        List<AiChatFileItem> files = picked.stream()
                .map(map::get)
                .filter(Objects::nonNull)
                .toList();

        String summary = (llmResult.getSummary() == null || llmResult.getSummary().isBlank())
                ? "관련 파일 후보 중 상위 결과입니다."
                : llmResult.getSummary();

        return new AiChatFileResponse(summary, files);
    }

    // =========================================================
    // ✅ Prompt Builders
    // =========================================================

    private String buildPromptForFiles(List<ChatAttachmentSearchRow> candidates, String query) {
        StringBuilder sb = new StringBuilder();

        sb.append("""
너는 팀 내부 채팅 파일을 찾아주는 업무 보조 AI다.
정확한 근거보다, 맥락상 가장 관련 있는 파일을 고르는 것이 목표다.
반드시 한국어로, 반드시 JSON만 반환하라.
주의: attachmentIds는 아래 목록의 [] 안에 있는 "실제 첨부파일 ID"만 사용하라.

[첨부파일 후보 목록]
""");

        for (ChatAttachmentSearchRow r : candidates) {
            sb.append("[")
                    .append(r.attachmentId())
                    .append("] ")
                    .append("(roomId=").append(r.roomId()).append(") ")
                    .append("(messageId=").append(r.messageId()).append(") ")
                    .append("(").append(r.createdAt()).append(") ")
                    .append("name=").append(r.originalName())
                    .append(" | context=").append(safe(r.messageSnippet()))
                    .append("\n");
        }

        sb.append("\n[사용자 질문]\n");
        sb.append(query);

        sb.append("""

위 질문과 가장 관련 있는 파일을 최대 5개만 선택하고,
어떤 파일들을 골랐는지 한 줄 요약(summary)을 작성하라.
JSON 형식으로만 응답하라.

{
  "summary": "...",
  "attachmentIds": [5000, 4991, 4988]
}
""");

        return sb.toString();
    }

    private String buildPromptForFileItems(List<AiChatFileItem> candidates, String query) {
        StringBuilder sb = new StringBuilder();

        sb.append("""
너는 팀 내부 채팅과 첨부파일을 기억해주는 업무 보조 AI다.

사용자의 질문은 반드시 파일명만 직접 언급하지 않을 수 있다.
날짜, 대화 상대, 상황 설명, 업무 맥락을 통해
"그때 이야기하면서 공유된 파일"을 찾는 것이 목표다.

다음과 같은 단서들을 종합적으로 고려하라:
- 파일명(originalName)
- 파일이 첨부된 메시지 내용(messageSnippet)
- 파일 업로드 시점(createdAt)
- 같은 메시지/같은 흐름에서 언급된 업무 맥락
- 사용자가 말한 날짜, 사람, 사건, 업무 흐름과의 유사성
- (중요) 대화 시점과 업로드 시점이 근접한 파일은 우선 고려하라

⚠️ 주의
- attachmentIds는 반드시 아래 후보 목록의 [] 안에 있는 실제 ID만 사용하라.
- 추측으로 새로운 ID를 만들어내지 마라.
- 가장 관련 있는 파일만 최대 5개 선택하라.
- 반드시 한국어로, 반드시 JSON만 반환하라.

[첨부파일 후보 목록]
""");

        for (AiChatFileItem r : candidates) {
            sb.append("[")
                    .append(r.attachmentId())
                    .append("] ")
                    .append("(roomId=").append(r.roomId()).append(") ")
                    .append("(messageId=").append(r.messageId()).append(") ")
                    .append("(").append(r.createdAt()).append(") ")
                    .append("name=").append(r.originalName())
                    .append(" | context=").append(safe(r.messageSnippet()))
                    .append("\n");
        }

        sb.append("\n[사용자 질문]\n");
        sb.append(query);

        sb.append("""

위 질문과 가장 관련 있는 파일을 최대 5개만 선택하고,
왜 이 파일들이 관련 있는지 한 줄 요약(summary)을 작성하라.
JSON 형식으로만 응답하라.

{
  "summary": "...",
  "attachmentIds": [5000, 4991, 4988]
}
""");

        return sb.toString();
    }

    // =========================================================
    // helpers
    // =========================================================

    private String toLikePattern(String query) {
        if (query == null) return "";

        String original = query.trim();
        String s = original;

        s = s.replaceAll("(찾아줘|찾아\\s*줘|찾아|검색해줘|검색\\s*해줘|검색|보여줘|보여\\s*줘|좀|해줘|해주세요|주세요|파일|첨부|문서)", " ");
        s = s.replaceAll("\\s+", " ").trim();

        if (s.isEmpty()) s = original;

        String[] parts = s.split("\\s+");
        String base = String.join("%", parts);

        if (parts.length == 1) {
            String token = parts[0];
            if (token.matches("[0-9A-Za-z가-힣]{4,}")) {
                int cut = token.length() == 4 ? 2 : token.length() / 2;
                String a = token.substring(0, cut);
                String b = token.substring(cut);
                base = a + "%" + b;
            }
        }

        return base;
    }

    private String safe(String s) {
        if (s == null) return "";
        String t = s.replace("\n", " ").replace("\r", " ");
        return t.length() > 120 ? t.substring(0, 120) + "..." : t;
    }
}
