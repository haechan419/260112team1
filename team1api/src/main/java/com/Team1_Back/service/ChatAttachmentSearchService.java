package com.Team1_Back.service;

import com.Team1_Back.dto.ChatAttachmentSearchRow;
import com.Team1_Back.repository.ChatAttachmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatAttachmentSearchService {

        private final ChatAttachmentRepository chatAttachmentRepository;

    public List<ChatAttachmentSearchRow> searchGlobal(Long userId, String q, Integer limit, Integer offset) {
        if (q == null || q.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "q is required");
        }

        int safeLimit = (limit == null) ? 20 : Math.min(Math.max(limit, 1), 50);
        int safeOffset = (offset == null) ? 0 : Math.max(offset, 0);

        // ✅ 맥락 윈도우 (기본 120초 추천: 텍스트 → 파일 올리기 흐름 커버됨)
        int ctxSeconds = 120;

        List<ChatAttachmentRepository.ChatAttachmentSearchView> rows =
                chatAttachmentRepository.searchMyChatAttachmentsWithContext(
                        userId,
                        q.trim(),
                        ctxSeconds,
                        safeLimit,
                        safeOffset
                );

        return rows.stream()
                .map(v -> new ChatAttachmentSearchRow(
                        v.getAttachmentId(),
                        v.getRoomId(),
                        v.getMessageId(),
                        v.getUploaderId(),
                        v.getOriginalName(),
                        v.getMimeType(),
                        v.getFileSize(),
                        v.getFileUrl(),
                        v.getCreatedAt().toLocalDateTime(),
                        v.getMessageSnippet()
                ))
                .toList();
    }

}

