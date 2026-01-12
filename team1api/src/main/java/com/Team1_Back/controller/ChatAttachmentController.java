package com.Team1_Back.controller;


import com.Team1_Back.dto.UploadMessageWithAttachmentsResponse;
import com.Team1_Back.dto.UserDTO;
import com.Team1_Back.service.ChatAttachmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatAttachmentController {

    private final ChatAttachmentService chatAttachmentService;

    /**
     * multipart/form-data
     * - content: optional
     * - files: multiple (required=false로 둬도 됨)
     *
     * response:
     * { ok: true, messageId: 123, attachments: [...] }
     */
    @PostMapping("/rooms/{roomId}/attachments")
    public ResponseEntity<?> uploadAttachments(
            @PathVariable Long roomId,
            @RequestParam(value = "content", required = false) String content,
            @RequestPart(value = "files", required = false) List<MultipartFile> files
    ) throws Exception {

        Long meId = getMyId(); // ✅ JWTCheckFilter가 넣어준 UserDTO에서 꺼냄

        UploadMessageWithAttachmentsResponse resp =
                chatAttachmentService.uploadWithMessage(roomId, meId, content, files);

        return ResponseEntity.ok(Map.of(
                "ok", true,
                "messageId", resp.getMessageId(),
                "attachments", resp.getAttachments()
        ));
    }

    private Long getMyId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new RuntimeException("Unauthenticated");
        }

        Object principal = auth.getPrincipal();

        // ✅ 우리 프로젝트는 principal = UserDTO
        if (principal instanceof UserDTO userDTO) {
            if (userDTO.getId() == null) throw new RuntimeException("No user id in token");
            return userDTO.getId();
        }

        // 혹시 익명/다른 타입 들어오는 경우 대비
        throw new RuntimeException("Invalid principal type: " + principal.getClass());
    }
}
