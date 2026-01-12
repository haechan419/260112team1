package com.Team1_Back.service;

import com.Team1_Back.ai.LlmRouter;
import com.Team1_Back.dto.AiContextMessageDto;
import com.Team1_Back.dto.AiContextResponse;
import com.Team1_Back.dto.LlmResult;
import com.Team1_Back.domain.ChatMessage;
import com.Team1_Back.repository.ChatMessageRepository;
import com.Team1_Back.repository.ChatRoomMemberRepository;
import com.Team1_Back.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
public class AiContextService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final LlmRouter llmRouter;

    public AiContextResponse findContext(Long roomId, String query) {

        Long me = SecurityUtil.currentUserId();
        if (me == null) throw new AccessDeniedException("UNAUTHORIZED");

        boolean isMember = chatRoomMemberRepository.existsByIdRoomIdAndIdUserId(roomId, me);
        if (!isMember) throw new AccessDeniedException("FORBIDDEN");

        List<ChatMessage> recentMessages =
                chatMessageRepository.findTop80ByRoomIdOrderByCreatedAtDesc(roomId);

        if (recentMessages.isEmpty()) {
            return new AiContextResponse(
                    "이 채팅방에는 메시지가 없어 맥락을 찾을 수 없습니다.",
                    List.of()
            );
        }

        String prompt = buildPromptWithRoom(recentMessages, query, false);

        LlmResult llmResult = llmRouter.ask(prompt);

        Set<Long> pickedIds = llmResult.getMessageIds().stream().collect(Collectors.toSet());

        List<AiContextMessageDto> messages =
                recentMessages.stream()
                        .filter(m -> pickedIds.contains(m.getId()))
                        .map(m -> new AiContextMessageDto(
                                m.getId(),
                                m.getContent(),
                                m.getCreatedAt(),
                                m.getRoomId() // ✅ roomId 포함
                        ))
                        .toList();

        return new AiContextResponse(llmResult.getSummary(), messages);
    }

    // ✅ 신규: 채팅방 안 열어도 "내 전체 채팅"에서 찾기
    public AiContextResponse findContextGlobal(String query) {

        Long me = SecurityUtil.currentUserId();
        if (me == null) throw new AccessDeniedException("UNAUTHORIZED");

        // 1) 내가 속한 모든 방의 최근 메시지 후보 가져오기
        List<ChatMessage> recentMessages = chatMessageRepository.findRecentMessagesForUser(me, 200);

        if (recentMessages.isEmpty()) {
            return new AiContextResponse(
                    "내가 속한 채팅방에 메시지가 없어 맥락을 찾을 수 없습니다.",
                    List.of()
            );
        }

        // 2) 프롬프트: roomId까지 같이 넣어서 LLM이 '어느 방인지'도 참고 가능하게
        String prompt = buildPromptWithRoom(recentMessages, query, true);

        // 3) LLM 호출
        LlmResult llmResult = llmRouter.ask(prompt);

        // 4) 선택된 메시지 매핑
        Set<Long> pickedIds = llmResult.getMessageIds().stream().collect(Collectors.toSet());

        List<AiContextMessageDto> messages =
                recentMessages.stream()
                        .filter(m -> pickedIds.contains(m.getId()))
                        .map(m -> new AiContextMessageDto(
                                m.getId(),
                                m.getContent(),
                                m.getCreatedAt(),
                                m.getRoomId()
                        ))
                        .toList();

        return new AiContextResponse(llmResult.getSummary(), messages);
    }

    // ✅ 기존 buildPrompt를 확장: Global일 때 roomId도 보여주기
    private String buildPromptWithRoom(List<ChatMessage> messages, String query, boolean global) {
        StringBuilder sb = new StringBuilder();

        sb.append("""
            너는 팀 내부 채팅을 기억해주는 업무 보조 AI다.
            정확한 근거보다, 맥락상 가장 관련 있는 대화를 찾는 것이 목표다.
            반드시 한국어로, 반드시 JSON만 반환하라.
            주의: messageIds는 아래 채팅 목록의 [] 안에 있는 "실제 메시지 ID"만 사용하라.
            """);

        if (global) {
            sb.append("""
                
                지금은 여러 채팅방의 메시지가 섞여 있다.
                각 메시지에는 (roomId=숫자)가 붙어 있다.
                사용자 질문과 가장 관련 있는 메시지를 고르되, 방이 여러 개면 핵심 방 1~2개에 집중해라.
                """);
        }

        sb.append("\n\n[채팅 메시지 목록]\n");

        for (ChatMessage m : messages) {
            sb.append("[")
                    .append(m.getId())
                    .append("] ")
                    .append("(roomId=").append(m.getRoomId()).append(") ")  // ✅ room 표시
                    .append("(").append(m.getCreatedAt()).append(") ")
                    .append("user_").append(m.getSenderId())
                    .append(": ")
                    .append(m.getContent())
                    .append("\n");
        }

        sb.append("\n[사용자 질문]\n");
        sb.append(query);

        sb.append("""
            
            위 질문과 가장 관련 있는 메시지를 최대 3개만 선택하고,
            무슨 얘기였는지 한 줄 요약을 작성해라.
            JSON 형식으로 응답하라.
            
            {
              "summary": "...",
              "messageIds": [100, 99, 97]
            }
            """);

        return sb.toString();
    }
}
