package com.Team1_Back.service;

import com.Team1_Back.ai.LlmRouter;
import com.Team1_Back.domain.ChatMessage;
import com.Team1_Back.dto.AiContextMessageDto;
import com.Team1_Back.dto.AiContextResponse;
import com.Team1_Back.dto.LlmResult;
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

    private String buildPromptWithRoom(List<ChatMessage> messages, String query, boolean global) {
        StringBuilder sb = new StringBuilder();

        sb.append("""
                너는 팀 내부 채팅을 기억해주는 업무 보조 AI다.
                단순한 키워드 검색이 아니라, 사람이 기억을 더듬듯
                "그때 어떤 맥락의 대화였는지"를 복원하는 것이 목표다.
                
                사용자의 질문은 다음과 같은 형태일 수 있다:
                - 특정 날짜나 시기 (예: 1월 초, 지난주, 며칠 전)
                - 특정 사람과의 대화 (예: ○○님이랑 이야기했을 때)
                - 업무 상황 설명 (예: 승인금액 정리하다가, 회의 끝나고)
                - 파일명을 직접 말하지 않는 간접적인 표현
                
                따라서 **질문에 정확한 단어가 없더라도**
                대화의 맥락, 흐름, 시점을 종합적으로 고려해
                가장 관련 있는 메시지를 선택하라.
                
                특히 다음 단서들을 함께 고려하라:
                - 메시지 내용(content)
                - 메시지 작성 시점(createdAt)
                - 같은 주제의 연속된 대화 흐름
                - 사용자가 언급한 날짜·사람·업무 상황과의 유사성
                
                ⚠️ 매우 중요
                - messageIds는 반드시 아래 목록의 [] 안에 있는 실제 메시지 ID만 사용하라.
                - 추측으로 새로운 ID를 만들어내지 마라.
                - 가장 관련 있는 메시지만 최대 3개 선택하라.
                - 반드시 한국어로, 반드시 JSON만 반환하라.
                """);

        if (global) {
            sb.append("""
                    
                    지금은 여러 채팅방의 메시지가 섞여 있다.
                    각 메시지에는 (roomId=숫자)가 함께 제공된다.
                    
                    사용자 질문과 가장 관련 있는 메시지를 고르되,
                    가능하면 **하나의 핵심 채팅방 또는 1~2개의 방**에 집중하라.
                    """);
        }

        sb.append("\n\n[채팅 메시지 목록]\n");

        for (ChatMessage m : messages) {
            sb.append("[")
                    .append(m.getId())
                    .append("] ")
                    .append("(roomId=").append(m.getRoomId()).append(") ")
                    .append("(").append(m.getCreatedAt()).append(") ")
                    .append("user_").append(m.getSenderId())
                    .append(": ")
                    .append(m.getContent())
                    .append("\n");
        }

        sb.append("\n[사용자 질문]\n");
        sb.append(query);

        sb.append("""
                
                위 질문과 가장 관련 있는 메시지를 선택하라.
                
                - 질문에 등장하는 날짜, 사람, 상황과 **직접적으로 일치하지 않아도**
                  맥락상 연결된 대화라면 선택할 수 있다.
                - "그때 이야기하면서", "그 이후에", "그 전날" 같은 표현은
                  createdAt을 기준으로 자연스럽게 추론해도 된다.
                
                선택한 메시지들이 어떤 맥락의 대화였는지
                한 줄로 요약하라.
                
                JSON 형식으로만 응답하라.
                
                {
                  "summary": "...",
                  "messageIds": [100, 99, 97]
                }
                """);

        return sb.toString();
    }
}