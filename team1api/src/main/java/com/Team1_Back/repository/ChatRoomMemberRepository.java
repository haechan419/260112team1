package com.Team1_Back.repository;

import com.Team1_Back.domain.ChatRoomMember;
import com.Team1_Back.domain.ChatRoomMemberId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, ChatRoomMemberId> {

    boolean existsByIdRoomIdAndIdUserId(Long roomId, Long userId);

    Optional<ChatRoomMember> findByIdRoomIdAndIdUserId(Long roomId, Long userId);

    long countByIdRoomId(Long roomId);

    @Query(value = "select user_id from chat_room_member where room_id = :roomId", nativeQuery = true)
    List<Long> findUserIdsByRoomId(@Param("roomId") Long roomId);

    List<ChatRoomMember> findAllByIdRoomId(Long roomId);

    @Query(value = """
        SELECT CASE WHEN EXISTS (
          SELECT 1 FROM chat_room_member rm
          WHERE rm.room_id = :roomId AND rm.user_id = :userId
        ) THEN 1 ELSE 0 END
        """, nativeQuery = true)
    int isMember(@Param("roomId") Long roomId, @Param("userId") Long userId);
}
