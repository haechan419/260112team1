package com.Team1_Back.service;

import com.Team1_Back.dto.ChatAttachmentDto;
import com.Team1_Back.dto.ChatMessageBroadcastDto;
import com.Team1_Back.dto.UploadMessageWithAttachmentsResponse;
import com.Team1_Back.domain.ChatAttachment;
import com.Team1_Back.domain.ChatMessage;
import com.Team1_Back.repository.ChatAttachmentRepository;
import com.Team1_Back.repository.ChatMessageRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ChatAttachmentService {

    private final ChatAttachmentRepository attachmentRepo;
    private final ChatMessageRepository messageRepo;
    private final ChatRoomSecurityService chatRoomSecurityService;
    private final SimpMessagingTemplate messagingTemplate;

    @Value("${app.upload.chat-base-dir}")
    private String baseDir;

    @Transactional
    public UploadMessageWithAttachmentsResponse uploadWithMessage(
            Long roomId,
            Long senderId,
            String content,
            List<MultipartFile> files
    ) throws Exception {

        // ✅ 멤버 권한 체크
        chatRoomSecurityService.assertMember(senderId, roomId);

        // ✅ message 먼저 생성 (content는 null 가능)
        ChatMessage msg = ChatMessage.builder()
                .roomId(roomId)
                .senderId(senderId)
                .content(StringUtils.hasText(content) ? content : "")
                .build();

        msg = messageRepo.save(msg);

        List<ChatAttachmentDto> attachments = new ArrayList<>();

        if (files != null) {
            for (MultipartFile file : files) {
                if (file == null || file.isEmpty()) continue;

                // ---------------------------
                // 1) 파일 메타
                // ---------------------------
                String originalName = Optional.ofNullable(file.getOriginalFilename()).orElse("file");
                String mimeType = Optional.ofNullable(file.getContentType())
                        .orElse(MediaType.APPLICATION_OCTET_STREAM_VALUE);
                long size = file.getSize();

                // ---------------------------
                // 2) 저장 파일명(UUID + 확장자 유지)
                // ---------------------------
                String ext = "";
                int dot = originalName.lastIndexOf('.');
                if (dot >= 0 && dot < originalName.length() - 1) {
                    ext = originalName.substring(dot);
                }
                String storedName = UUID.randomUUID() + ext;

                // ---------------------------
                // 3) 저장 경로: {baseDir}/{roomId}/{yyyy}/{MM}/{dd}/storedName
                // ---------------------------
                LocalDate now = LocalDate.now();
                Path dir = Paths.get(
                        baseDir,
                        String.valueOf(roomId),
                        String.valueOf(now.getYear()),
                        String.format("%02d", now.getMonthValue()),
                        String.format("%02d", now.getDayOfMonth())
                );
                Files.createDirectories(dir);

                Path target = dir.resolve(storedName);

                // ✅ 파일 저장 (덮어쓰기 허용)
                Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

                // ---------------------------
                // 4) DB 저장 (attachment)
                // ---------------------------
                ChatAttachment att = ChatAttachment.builder()
                        .message(msg)                  // FK message_id NOT NULL 대응
                        .roomId(roomId)
                        .uploaderId(senderId)
                        .originalName(originalName)
                        .storedName(storedName)
                        .mimeType(mimeType)
                        .fileSize(size)
                        .storageType("LOCAL")
                        .filePath(target.toString())
                        .fileUrl("") // id 생성 후 채움
                        .build();

                att = attachmentRepo.save(att);

                // ✅ 저장 후 id 기반 다운로드 URL 확정
                att.setFileUrl("/api/files/chat/" + att.getId() + "/download");
                attachmentRepo.save(att);

                // ---------------------------
                // 5) 응답 DTO
                // ---------------------------
                attachments.add(ChatAttachmentDto.builder()
                        .attachmentId(att.getId())
                        .originalName(originalName)
                        .mimeType(mimeType)
                        .size(size)
                        .url(att.getFileUrl())
                        .build());
            }
        }

        ChatMessageBroadcastDto payload = ChatMessageBroadcastDto.builder()
                .type("MESSAGE")
                .roomId(roomId)
                .messageId(msg.getId())
                .senderId(senderId)
                .content(msg.getContent() == null ? "" : msg.getContent())
                .createdAt(msg.getCreatedAt())
                .attachments(attachments)
                .build();

        // ✅ 프론트 subscribeRoom(): /topic/room/${roomId}
        messagingTemplate.convertAndSend("/topic/room/" + roomId, payload);

        return UploadMessageWithAttachmentsResponse.builder()
                .messageId(msg.getId())
                .attachments(attachments)
                .build();
    }
}
