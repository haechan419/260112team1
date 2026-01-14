package com.Team1_Back.repository;

// package com.Team1_Back.chat.repository;


import com.Team1_Back.domain.ChatAttachment;
import com.Team1_Back.dto.AiChatFileItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ChatAttachmentRepository extends JpaRepository<ChatAttachment, Long> {
    List<ChatAttachment> findByMessage_Id(Long messageId);
    Optional<ChatAttachment> findByIdAndDeletedAtIsNull(Long id);


    public interface ChatAttachmentSearchView {
        Long getAttachmentId();
        Long getRoomId();
        Long getMessageId();
        Long getUploaderId();
        String getOriginalName();
        String getMimeType();
        Long getFileSize();
        String getFileUrl();
        java.sql.Timestamp getCreatedAt();   // ✅ Timestamp로!
        String getMessageSnippet();
    }

    @Query(value = """
        SELECT
          a.id            AS attachmentId,
          a.room_id       AS roomId,
          a.message_id    AS messageId,
          a.uploader_id   AS uploaderId,
          a.original_name AS originalName,
          a.mime_type     AS mimeType,
          a.file_size     AS fileSize,
          a.file_url      AS fileUrl,
          a.created_at    AS createdAt,
          m.content       AS messageSnippet
        FROM chat_attachment a
        JOIN chat_message m ON m.id = a.message_id
        WHERE a.deleted_at IS NULL
          AND EXISTS (
            SELECT 1 FROM chat_room_member rm
            WHERE rm.room_id = a.room_id AND rm.user_id = :userId
          )
          AND (
            a.original_name COLLATE utf8mb4_unicode_ci LIKE CONCAT('%', :q COLLATE utf8mb4_unicode_ci, '%')
            OR m.content    COLLATE utf8mb4_unicode_ci LIKE CONCAT('%', :q COLLATE utf8mb4_unicode_ci, '%')
          )
        ORDER BY a.created_at DESC
        LIMIT :limit OFFSET :offset
        """, nativeQuery = true)
    List<ChatAttachmentSearchView> searchMyChatAttachments(
            @Param("userId") Long userId,
            @Param("q") String q,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    @Query("""
    select new com.Team1_Back.dto.AiChatFileItem(
        a.id,
        a.roomId,
        m.id,
        a.originalName,
        a.fileUrl,
        a.createdAt,
        coalesce(m.content, '')
    )
    from ChatAttachment a
    left join a.message m
    where a.deletedAt is null
      and a.roomId = :roomId
      and a.createdAt between :from and :to
    order by a.createdAt desc
""")
    List<AiChatFileItem> findItemsInWindow(
            @Param("roomId") Long roomId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    /**
     * ✅ 전역 첨부 검색 (내가 속한 방만)
     * - 직접 매칭: 파일명(original_name) / 첨부 메시지 content
     * - 맥락 매칭: "검색어가 들어간 텍스트 메시지" 기준 ±ctxSeconds 내 메시지에 달린 첨부
     */
    @Query(value = """
        WITH hit_msgs AS (
            SELECT m.id        AS hit_id,
                   m.room_id   AS room_id,
                   m.created_at AS hit_at
            FROM chat_message m
            JOIN chat_room_member crm
              ON crm.room_id = m.room_id
             AND crm.user_id = :userId
            WHERE m.content LIKE CONCAT('%', :q, '%')
        ),
        ctx_msgs AS (
            SELECT m2.id AS message_id,
                   m2.room_id AS room_id
            FROM hit_msgs h
            JOIN chat_message m2
              ON m2.room_id = h.room_id
             AND ABS(TIMESTAMPDIFF(SECOND, m2.created_at, h.hit_at)) <= :ctxSeconds
        ),
        candidates AS (
            SELECT a.id AS attachment_id,
                   a.room_id AS room_id,
                   a.message_id AS message_id,
                   a.uploader_id AS uploader_id,
                   a.original_name AS original_name,
                   a.mime_type AS mime_type,
                   a.file_size AS file_size,
                   a.created_at AS created_at,
                   COALESCE(m.content, '') AS message_snippet,
                   CASE
                      WHEN a.original_name LIKE CONCAT('%', :q, '%') THEN 'FILENAME'
                      WHEN m.content LIKE CONCAT('%', :q, '%') THEN 'ATT_MSG'
                      WHEN EXISTS (SELECT 1 FROM ctx_msgs c WHERE c.message_id = a.message_id) THEN 'CONTEXT'
                      ELSE 'UNKNOWN'
                   END AS match_reason
            FROM chat_attachment a
            JOIN chat_room_member crm
              ON crm.room_id = a.room_id
             AND crm.user_id = :userId
            LEFT JOIN chat_message m
              ON m.id = a.message_id
            WHERE
                 a.original_name LIKE CONCAT('%', :q, '%')
              OR m.content LIKE CONCAT('%', :q, '%')
              OR EXISTS (SELECT 1 FROM ctx_msgs c WHERE c.message_id = a.message_id)
        )
        SELECT DISTINCT
               c.attachment_id AS attachmentId,
               c.room_id AS roomId,
               c.message_id AS messageId,
               c.uploader_id AS uploaderId,
               c.original_name AS originalName,
               c.mime_type AS mimeType,
               c.file_size AS fileSize,
               CONCAT('/api/files/chat/', c.attachment_id, '/download') AS fileUrl,
               c.created_at AS createdAt,
               c.message_snippet AS messageSnippet,
               c.match_reason AS matchReason
        FROM candidates c
        ORDER BY c.created_at DESC
        LIMIT :limit OFFSET :offset
        """, nativeQuery = true)
    List<ChatAttachmentSearchView> searchMyChatAttachmentsWithContext(
            @Param("userId") Long userId,
            @Param("q") String q,
            @Param("ctxSeconds") int ctxSeconds,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

}
