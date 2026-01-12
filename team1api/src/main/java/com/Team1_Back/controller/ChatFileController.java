package com.Team1_Back.controller;

import com.Team1_Back.domain.ChatAttachment;
import com.Team1_Back.dto.UserDTO;
import com.Team1_Back.repository.ChatAttachmentRepository;
import com.Team1_Back.service.ChatRoomSecurityService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/files/chat")
public class ChatFileController {

    private final ChatAttachmentRepository attachmentRepo;
    private final ChatRoomSecurityService chatRoomSecurityService;

    /**
     * GET /api/files/chat/{attachmentId}/download
     * - attachmentId로 첨부 조회
     * - 방 멤버십 체크
     * - 파일 스트리밍 다운로드
     */
    @GetMapping("/{attachmentId}/download")
    public ResponseEntity<Resource> download(
            @PathVariable Long attachmentId,
            @RequestParam(name = "inline", required = false, defaultValue = "false") boolean inline,
            @AuthenticationPrincipal UserDTO user
    ) {
        if (user == null || user.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "unauthorized");
        }
        Long meId = user.getId();

        ChatAttachment att = attachmentRepo.findById(attachmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "attachment not found"));

        // ✅ 방 멤버십 체크 (핵심 보안)
        Long roomId = att.getRoomId();
        if (roomId == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "attachment roomId is null");
        }
        // (주의) 너희 SecurityService 시그니처가 assertMember(userId, roomId)이면 아래처럼
        chatRoomSecurityService.assertMember(meId, roomId);

        // ✅ 파일 존재 확인
        String filePathStr = att.getFilePath();
        if (filePathStr == null || filePathStr.isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "filePath is empty");
        }

        Path filePath = Paths.get(filePathStr);
        if (!Files.exists(filePath) || !Files.isReadable(filePath)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "file missing");
        }

        // ✅ Content-Type
        String mime = att.getMimeType();
        MediaType mediaType;
        try {
            mediaType = (mime != null && !mime.isBlank())
                    ? MediaType.parseMediaType(mime)
                    : MediaType.APPLICATION_OCTET_STREAM;
        } catch (Exception e) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        // ✅ Content-Disposition (파일명 깨짐 방지: filename* 사용)
        String originalName = (att.getOriginalName() != null && !att.getOriginalName().isBlank())
                ? att.getOriginalName()
                : filePath.getFileName().toString();

        ContentDisposition disposition = (inline
                ? ContentDisposition.inline()
                : ContentDisposition.attachment())
                .filename(originalName, java.nio.charset.StandardCharsets.UTF_8)
                .build();

        Resource resource = new FileSystemResource(filePath);

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                // 캐시 정책은 상황 따라 조절 (여긴 보수적으로 no-store)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(resource);
    }
}
