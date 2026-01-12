package com.Team1_Back.controller;

import com.Team1_Back.dto.ChatMessageResponse;
import com.Team1_Back.dto.ChatSendRequest;
import com.Team1_Back.security.CurrentUser;
import com.Team1_Back.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatStompController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.send")
    public void send(ChatSendRequest req) {
        Long senderId = CurrentUser.id();


        ChatMessageResponse saved = chatService.sendMessage(req.getRoomId(), senderId, req.getContent());

        // 방 구독자에게 broadcast
        messagingTemplate.convertAndSend("/topic/chat/rooms/" + req.getRoomId(), saved);
    }
}
