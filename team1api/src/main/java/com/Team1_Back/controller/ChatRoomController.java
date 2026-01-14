package com.Team1_Back.controller;

import com.Team1_Back.dto.*;
import com.Team1_Back.security.CurrentUser;
import com.Team1_Back.service.ChatRoomCommandService;
import com.Team1_Back.service.ChatRoomQueryService;
import com.Team1_Back.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatRoomController {

    private final ChatService chatService;                 // 메시지/메타 등
    private final ChatRoomQueryService chatRoomQueryService;
    private final ChatRoomCommandService chatRoomCommandService; // DM/그룹/초대

    @GetMapping("/rooms")
    public List<ChatRoomListItemResponse> myRooms() {
        Long meId = CurrentUser.id();
        return chatRoomQueryService.myRooms(meId);
    }

    @PostMapping("/rooms/dm")
    public Map<String, Object> createDm(@RequestBody CreateDmRequest req) {
        Long meId = CurrentUser.id();

        // ✅ 무조건 이 로직으로 통일 (멤버 insert 보장)
        Long roomId = chatService.getOrCreateDirectRoom(meId, req.getTargetUserId());

        return Map.of("roomId", roomId);
    }

    @PostMapping("/rooms/group")
    public Map<String, Object> createGroup(@RequestBody CreateGroupRequest req) {
        Long meId = CurrentUser.id();
        Long roomId = chatRoomCommandService.createGroup(meId, req.getMemberUserIds());
        return Map.of("roomId", roomId);
    }

    @PostMapping("/rooms/{roomId}/invite")
    public Map<String, Object> invite(@PathVariable Long roomId, @RequestBody InviteRequest req) {
        Long meId = CurrentUser.id();
        chatRoomCommandService.invite(meId, roomId, req.getUserIds());
        return Map.of("success", true);
    }

    @PostMapping("/rooms/{roomId}/messages")
    public ChatMessageResponse sendMessage(
            @PathVariable Long roomId,
            @RequestBody ChatMessageSendRequest req
    ) {
        Long meId = CurrentUser.id();
        return chatService.sendMessage(roomId, meId, req.getContent());
    }

//    @PostMapping("/rooms/{roomId}/read")
//    public Map<String, Object> read(
//            @PathVariable Long roomId,
//            @RequestBody ChatReadRequest req
//    ) {
//        Long meId = CurrentUser.id();
//        chatService.updateRead(roomId, meId, req.getLastReadMessageId());
//        return Map.of("success", true);
//    }

    @DeleteMapping("/rooms/{roomId}")
    public Map<String, Object> leaveRoom(@PathVariable Long roomId) {
        Long meId = CurrentUser.id();
        chatRoomCommandService.leaveRoom(meId, roomId);
        return Map.of("success", true);
    }

//    @GetMapping("/rooms/{roomId}/messages")
//    public List<ChatMessageResponse> getMessages(
//            @PathVariable Long roomId,
//            @RequestParam(defaultValue = "30") int limit
//    ) {
//        Long meId = CurrentUser.id();
//        return chatService.getMessages(roomId, meId, limit);
//    }



}
