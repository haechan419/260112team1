package com.Team1_Back.service;

import com.Team1_Back.dto.AiChatFileItem;
import com.Team1_Back.repository.ChatAttachmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatAttachmentQueryService {

    private final ChatAttachmentRepository chatAttachmentRepository;

    public List<AiChatFileItem> findDirectAttachmentsByMessages(List<Long> messageIds) {

        return messageIds.stream()
                .flatMap(id -> chatAttachmentRepository.findByMessage_Id(id).stream())
                .filter(a -> a.getDeletedAt() == null)
                .map(a -> new AiChatFileItem(
                        a.getId(),
                        a.getRoomId(),
                        a.getMessage().getId(),
                        a.getOriginalName(),
                        a.getFileUrl(),
                        a.getCreatedAt(),
                        a.getMessage().getContent()
                ))
                .toList();
    }

    public List<AiChatFileItem> findNearbyAttachments(
            Long roomId,
            LocalDateTime from,
            LocalDateTime to
    ) {
        if (roomId == null || from == null || to == null) return List.of();
        return chatAttachmentRepository.findItemsInWindow(roomId, from, to);
    }

}
