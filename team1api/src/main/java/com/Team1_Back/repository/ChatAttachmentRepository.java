package com.Team1_Back.repository;

// package com.Team1_Back.chat.repository;


import com.Team1_Back.domain.ChatAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatAttachmentRepository extends JpaRepository<ChatAttachment, Long> {
    List<ChatAttachment> findByMessage_Id(Long messageId);
    Optional<ChatAttachment> findByIdAndDeletedAtIsNull(Long id);
}
