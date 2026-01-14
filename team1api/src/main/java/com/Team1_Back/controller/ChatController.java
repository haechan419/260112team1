package com.Team1_Back.controller;

import com.Team1_Back.dto.ChatRoomMetaResponse;
import com.Team1_Back.service.ChatService;
import com.Team1_Back.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @GetMapping("/rooms/{roomId}/meta")
    public ChatRoomMetaResponse roomMeta(@PathVariable Long roomId) {
        Long meId = SecurityUtil.currentUserId();
        if (meId == null) throw new RuntimeException("Unauthenticated");
        return chatService.getRoomMeta(roomId, meId);
    }
}

