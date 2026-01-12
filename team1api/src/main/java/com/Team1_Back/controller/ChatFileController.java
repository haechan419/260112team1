package com.Team1_Back.controller;

import com.Team1_Back.domain.ChatAttachment;
import com.Team1_Back.dto.UserDTO;
import com.Team1_Back.repository.ChatAttachmentRepository;
import com.Team1_Back.service.ChatRoomSecurityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/files/chat")
@Slf4j
public class ChatFileController {

    private final ChatAttachmentRepository attachmentRepo;
    private final ChatRoomSecurityService chatRoomSecurityService;

    /**
     * ✅ 채팅 파일 저장 베이스 디렉토리
     * - application.properties: app.upload.chat-base-dir=./uploads/chat
     */
    @Value("${app.upload.chat-base-dir:./uploads/chat}")
    private String chatBaseDir;

    /**
     * GET /api/files/chat/{attachmentId}/download?inline=true|false
     *
     * ✅ 동작 목표
     * 1) attachmentId로 DB 조회
     * 2) 방 멤버십 체크 (보안)
     * 3) 디스크 경로 해석:
     *    - storedName 있으면: {chatBaseDir}/{storedName} (정석)
     *    - 없으면 filePath로 호환:
     *      - "/uploads/chat/xxx" 같은 URL 경로면 prefix 제거 후 chatBaseDir에 붙임
     *      - 절대경로면 그대로(단, baseDir 밖 탈출은 차단)
     *      - 상대경로면 chatBaseDir에 붙임
     * 4) 파일 스트리밍 다운로드
     */
    @GetMapping("/{attachmentId}/download")
    public ResponseEntity<Resource> download(
            @PathVariable Long attachmentId,
            @RequestParam(name = "inline", required = false, defaultValue = "false") boolean inline,
            @AuthenticationPrincipal UserDTO user
    ) {
        // =========================
        // 0) 인증
        // =========================
        if (user == null || user.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "unauthorized");
        }
        Long meId = user.getId();

        // =========================
        // 1) 첨부 조회
        // =========================
        ChatAttachment att = attachmentRepo.findById(attachmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "attachment not found"));

        // =========================
        // 2) 방 멤버십 체크
        // =========================
        Long roomId = att.getRoomId();
        if (roomId == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "attachment roomId is null");
        }
        chatRoomSecurityService.assertMember(meId, roomId);

        // =========================
        // 3) 디스크 경로 해석
        // =========================
        Path base = Paths.get(chatBaseDir).toAbsolutePath().normalize();
        Path resolved = resolveDiskPath(base, att);

        // ✅ 원인 추적용 로그 (지금 케이스에서 매우 중요)
        log.info("[CHAT-DL] attId={} roomId={} baseDir='{}' storedName='{}' filePath='{}' resolved='{}'",
                attachmentId,
                roomId,
                base,
                nullSafe(att.getStoredName()),
                nullSafe(att.getFilePath()),
                resolved
        );

        // =========================
        // 4) 파일 존재 체크
        // =========================
        if (!Files.exists(resolved) || !Files.isReadable(resolved)) {
            log.warn("[CHAT-DL] file missing attId={} resolved='{}' exists={} readable={}",
                    attachmentId, resolved, Files.exists(resolved), Files.isReadable(resolved));
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "file missing");
        }

        // =========================
        // 5) Content-Type
        // =========================
        MediaType mediaType = toMediaType(att.getMimeType(), resolved);

        // =========================
        // 6) Content-Disposition (파일명 UTF-8)
        // =========================
        String originalName = safeDownloadName(att.getOriginalName(), resolved.getFileName().toString());

        ContentDisposition disposition = (inline
                ? ContentDisposition.inline()
                : ContentDisposition.attachment())
                .filename(originalName, StandardCharsets.UTF_8)
                .build();

        Resource resource = new FileSystemResource(resolved);

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(resource);
    }

    // =========================================================
    // Path Resolver
    // =========================================================

    /**
     * ✅ path 결정 규칙
     * - storedName 우선 (정석)
     * - 없으면 filePath 호환:
     *   1) "/uploads/chat/xxx" or "uploads/chat/xxx" -> prefix 제거 후 baseDir + "xxx"
     *   2) 상대경로 -> baseDir + 상대경로
     *   3) 절대경로 -> 그대로 사용하되 baseDir 밖이면 차단(보안)
     */
    private Path resolveDiskPath(Path baseDir, ChatAttachment att) {

        // ✅ 1) filePath 우선 (현재 저장 구조에 맞음)
        String fp = trimOrNull(att.getFilePath());
        if (fp != null) {

            String rel = fp.replace("\\", "/").trim();

            // "./" 또는 ".\" 제거
            if (rel.startsWith("./")) rel = rel.substring(2);
            if (rel.startsWith(".\\")) rel = rel.substring(2);

            // leading "/" 제거
            if (rel.startsWith("/")) rel = rel.substring(1);

            // ✅ filePath가 "uploads/chat/..." 또는 "uploads/..." 포함할 때 baseDir(uploads/chat) 기준으로 맞추기
            if (rel.startsWith("uploads/chat/")) {
                rel = rel.substring("uploads/chat/".length()); // => "18/2026/..."
            } else if (rel.startsWith("uploads/")) {
                rel = rel.substring("uploads/".length()); // => "chat/18/..." or "18/..."
                if (rel.startsWith("chat/")) rel = rel.substring("chat/".length());
            } else if (rel.startsWith("chat/")) {
                // 혹시 "chat/..."로 오면 baseDir이 이미 chat이므로 제거
                rel = rel.substring("chat/".length());
            }

            Path candidate = baseDir.resolve(rel).normalize();
            return ensureInsideBase(baseDir, candidate);
        }

        // ✅ 2) fallback: storedName (구버전 호환)
        String stored = trimOrNull(att.getStoredName());
        if (stored != null) {
            Path p = baseDir.resolve(stored).normalize();
            return ensureInsideBase(baseDir, p);
        }

        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "filePath/storedName is empty");
    }

    /**
     * ✅ 보안: baseDir 밖으로 탈출하는 경로 차단
     */
    private Path ensureInsideBase(Path baseDir, Path candidate) {
        Path base = baseDir.toAbsolutePath().normalize();
        Path c = candidate.toAbsolutePath().normalize();
        if (!c.startsWith(base)) {
            log.warn("[CHAT-DL] path traversal blocked base='{}' candidate='{}'", base, c);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid file path");
        }
        return c;
    }

    // =========================================================
    // Helpers
    // =========================================================

    private MediaType toMediaType(String mimeFromDb, Path filePath) {
        // DB mime 우선
        if (mimeFromDb != null && !mimeFromDb.isBlank()) {
            try {
                return MediaType.parseMediaType(mimeFromDb);
            } catch (Exception ignored) {
            }
        }
        // fallback: Files.probeContentType
        try {
            String guessed = Files.probeContentType(filePath);
            if (guessed != null && !guessed.isBlank()) {
                return MediaType.parseMediaType(guessed);
            }
        } catch (Exception ignored) {
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }

    private String safeDownloadName(String originalName, String fallback) {
        String name = (originalName != null && !originalName.isBlank()) ? originalName.trim() : fallback;
        // 헤더 깨짐 방지: 줄바꿈 제거
        name = name.replace("\n", " ").replace("\r", " ").trim();
        return name.isBlank() ? "file" : name;
    }

    private String trimOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isBlank() ? null : t;
    }

    private String nullSafe(String s) {
        return s == null ? "" : s;
    }
}
