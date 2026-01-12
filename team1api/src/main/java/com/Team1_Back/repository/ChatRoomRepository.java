package com.Team1_Back.repository;

import com.Team1_Back.domain.ChatRoom;
import com.Team1_Back.repository.projection.ChatRoomListRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    @Query(value = """
        SELECT
            r.id AS roomId,
            r.type AS type,
            r.direct_key AS directKey,


            CASE
              WHEN r.type = 'DIRECT' THEN (
                SELECT u2.name
                FROM chat_room_member crm2
                JOIN users u2 ON u2.id = crm2.user_id
                WHERE crm2.room_id = r.id
                  AND crm2.user_id <> :meId
                LIMIT 1
              )
              ELSE (
                SELECT
                  CASE
                    WHEN COUNT(*) = 0 THEN '그룹채팅'
                    WHEN COUNT(*) <= 3 THEN GROUP_CONCAT(u3.name ORDER BY u3.name SEPARATOR ', ')
                    ELSE CONCAT(
                      SUBSTRING_INDEX(GROUP_CONCAT(u3.name ORDER BY u3.name SEPARATOR ', '), ', ', 3),
                      '…'
                    )
                  END
                FROM chat_room_member crm3
                JOIN users u3 ON u3.id = crm3.user_id
                WHERE crm3.room_id = r.id
                  AND crm3.user_id <> :meId
              )
            END AS partnerName,

            lm.id AS lastMessageId,
            lm.sender_id AS lastSenderId,
            lm.content AS lastContent,
            lm.created_at AS lastCreatedAt,

            (
              SELECT COUNT(*)
              FROM chat_message m2
              WHERE m2.room_id = r.id
                AND m2.deleted_at IS NULL
                AND m2.sender_id <> :meId
                AND (crm.last_read_message_id IS NULL OR m2.id > crm.last_read_message_id)
            ) AS unreadCount

        FROM chat_room r
        JOIN chat_room_member crm
          ON crm.room_id = r.id
         AND crm.user_id = :meId

        LEFT JOIN chat_message lm
          ON lm.room_id = r.id
         AND lm.deleted_at IS NULL
         AND lm.id = (
            SELECT MAX(m3.id)
            FROM chat_message m3
            WHERE m3.room_id = r.id
              AND m3.deleted_at IS NULL
         )

        ORDER BY COALESCE(lm.id, 0) DESC
        """, nativeQuery = true)
    List<ChatRoomListRow> findMyRoomList(@Param("meId") Long meId);

    Optional<ChatRoom> findByDirectKey(String directKey);
}
