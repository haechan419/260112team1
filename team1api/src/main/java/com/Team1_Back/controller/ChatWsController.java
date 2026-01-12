package com.Team1_Back.controller;

import com.Team1_Back.dto.ChatMessageResponse;
import com.Team1_Back.dto.ChatWsLeaveRequest;
import com.Team1_Back.dto.ChatWsSendRequest;
import com.Team1_Back.service.ChatRoomCommandService;
import com.Team1_Back.service.ChatRoomQueryService;
import com.Team1_Back.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
@RequiredArgsConstructor
public class ChatWsController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;
    private final ChatRoomCommandService chatRoomCommandService;
    private final ChatRoomQueryService chatRoomQueryService;

    // ✅ 메시지 전송
    @MessageMapping("/chat/send")
    public void send(ChatWsSendRequest req, java.security.Principal principal) {
        Long meId = Long.valueOf(principal.getName());

        ChatMessageResponse saved = chatService.sendMessage(req.getRoomId(), meId, req.getContent());

        // 방 구독자에게 메시지 전파
        messagingTemplate.convertAndSend("/topic/room/" + req.getRoomId(), saved);

        // 방 목록 업데이트 이벤트 (멤버 전원에게)
        chatService.broadcastRoomsChanged(req.getRoomId());
    }

    // ✅ 채팅방 나가기(삭제)
    @MessageMapping("/chat/leave")
    public void leave(ChatWsLeaveRequest req, java.security.Principal principal) {
        Long meId = Long.valueOf(principal.getName());

        chatRoomCommandService.leaveRoom(meId, req.getRoomId());

        // 나간 당사자에게 rooms refresh 신호
        messagingTemplate.convertAndSendToUser(
                String.valueOf(meId),
                "/queue/rooms",
                Map.of("type", "ROOMS_CHANGED")
        );

        // 남은 멤버들에게도 rooms refresh 신호
        chatService.broadcastRoomsChanged(req.getRoomId());
    }
}
