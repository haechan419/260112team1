package com.Team1_Back.controller;

import com.Team1_Back.dto.DirectRoomCreateRequest;
import com.Team1_Back.dto.DirectRoomCreateResponse;
import com.Team1_Back.dto.ReadUpdateRequest;
import com.Team1_Back.dto.ChatMessageResponse;
import com.Team1_Back.security.CurrentUser;
import com.Team1_Back.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatRestController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    @PostMapping("/rooms/direct")
    public DirectRoomCreateResponse createOrGetDirect(@RequestBody DirectRoomCreateRequest req) {
        Long meId = CurrentUser.id();

        Long roomId = chatService.getOrCreateDirectRoom(meId, req.getTargetUserId());
        return new DirectRoomCreateResponse(roomId);
    }

    @GetMapping("/rooms/{roomId}/messages")
    public List<ChatMessageResponse> messages(
            @PathVariable Long roomId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "30") int limit
    ) {
        Long meId = CurrentUser.id();
        return chatService.getMessages(roomId, meId, cursor, limit);
    }

    @PostMapping("/rooms/{roomId}/read")
    public void read(
            @PathVariable Long roomId,
            @RequestBody(required = false) ReadUpdateRequest req
    ) {
        Long meId = CurrentUser.id();

        Long lastReadMessageId = (req == null) ? null : req.getLastReadMessageId();

        chatService.updateRead(roomId, meId, lastReadMessageId);

        // ✅ READ 이벤트 브로드캐스트 (내 메시지 '1/2' 실시간 갱신용)
        messagingTemplate.convertAndSend(
                "/topic/room/" + roomId + "/read",
                Map.of(
                        "type", "READ",
                        "roomId", roomId,
                        "userId", meId,
                        "lastReadMessageId", lastReadMessageId
                )
        );
    }
}
