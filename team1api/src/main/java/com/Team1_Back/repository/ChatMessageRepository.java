package com.Team1_Back.repository;

import com.Team1_Back.domain.ChatMessage;
import com.Team1_Back.dto.MessageTimeView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // 최신 메시지부터 페이징: cursor(마지막으로 받은 messageId)보다 작은 것들을 가져오기
    @Query("""
        select m from ChatMessage m
        where m.roomId = :roomId
          and (:cursor is null or m.id < :cursor)
          and m.deletedAt is null
        order by m.id desc
    """)
    List<ChatMessage> findPage(Long roomId, Long cursor);

    @Query("""
        select max(m.id) from ChatMessage m
        where m.roomId = :roomId and m.deletedAt is null
    """)
    Long findLastMessageId(Long roomId);

    @Query(value = """
        SELECT MAX(m.id)
        FROM chat_message m
        WHERE m.room_id = :roomId
          AND m.deleted_at IS NULL
        """, nativeQuery = true)
    Optional<Long> findLatestMessageId(Long roomId);

    List<ChatMessage> findTop80ByRoomIdOrderByCreatedAtDesc(Long roomId);

    @Query(value = """
        SELECT m.*
        FROM chat_message m
        JOIN chat_room_member crm
          ON crm.room_id = m.room_id
        WHERE crm.user_id = :meId
        ORDER BY m.created_at DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<ChatMessage> findRecentMessagesForUser(@Param("meId") Long meId, @Param("limit") int limit);

    @Query(value = """
    select m.id
    from chat_message m
    where m.room_id = :roomId
      and m.deleted_at is null
    order by m.id desc
    limit :limit
""", nativeQuery = true)
    List<Long> findRecentIds(@Param("roomId") Long roomId, @Param("limit") int limit);


    @Query("""
select distinct m
from ChatMessage m
left join fetch m.attachments
where m.id in :ids
order by m.id desc
""")
    List<ChatMessage> findByIdInWithAttachments(@Param("ids") List<Long> ids);

    @Query(value = """
    select m.id
    from chat_message m
    where m.room_id = :roomId
      and (:cursor is null or m.id < :cursor)
      and m.deleted_at is null
    order by m.id desc
    limit :limit
""", nativeQuery = true)
    List<Long> findPageIds(@Param("roomId") Long roomId,
                           @Param("cursor") Long cursor,
                           @Param("limit") int limit);


    @Query("""
    select new com.Team1_Back.dto.MessageTimeView(m.id, m.roomId, m.createdAt)
    from ChatMessage m
    where m.id in :ids
""")
    List<MessageTimeView> findMessageTimes(@Param("ids") List<Long> ids);


}
