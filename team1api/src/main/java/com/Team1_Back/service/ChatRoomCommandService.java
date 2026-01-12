package com.Team1_Back.service;

import com.Team1_Back.domain.ChatRoom;
import com.Team1_Back.domain.ChatRoomMember;
import com.Team1_Back.domain.ChatRoomMemberId;
import com.Team1_Back.repository.ChatRoomMemberRepository;
import com.Team1_Back.repository.ChatRoomRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatRoomCommandService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;

    @Transactional
    public Long createDm(Long meId, Long targetId) {
        if (meId == null || targetId == null) throw new IllegalArgumentException("null id");
        if (meId.equals(targetId)) throw new IllegalArgumentException("cannot DM yourself");

        String directKey = makeDirectKey(meId, targetId);

        // ✅ 있으면 재사용
        return chatRoomRepository.findByDirectKey(directKey)
                .map(ChatRoom::getId)
                .orElseGet(() -> {
                    ChatRoom room = new ChatRoom();
                    room.setType("DIRECT");
                    room.setDirectKey(directKey);
                    room.setCreatedAt(LocalDateTime.now()); // created_at이 timestamp면 Instant/LocalDateTime 중 도메인 타입 맞추기
                    chatRoomRepository.save(room);

                    insertMember(room.getId(), meId);
                    insertMember(room.getId(), targetId);

                    return room.getId();
                });
    }

    @Transactional
    public Long createGroup(Long meId, List<Long> memberIds) {
        ChatRoom room = new ChatRoom();
        room.setType("GROUP");
        room.setDirectKey(null);
        room.setCreatedAt(LocalDateTime.now());
        chatRoomRepository.save(room);

        insertMember(room.getId(), meId);

        if (memberIds != null) {
            for (Long uid : memberIds.stream().distinct().toList()) {
                if (uid == null || uid.equals(meId)) continue;
                insertMember(room.getId(), uid);
            }
        }

        return room.getId();
    }

    @Transactional
    public void invite(Long meId, Long roomId, List<Long> userIds) {
        if (!chatRoomMemberRepository.existsByIdRoomIdAndIdUserId(roomId, meId)) {
            throw new SecurityException("not a member");
        }
        if (userIds == null || userIds.isEmpty()) return;

        for (Long uid : userIds.stream().distinct().toList()) {
            if (uid == null) continue;
            insertMember(roomId, uid);
        }
    }

    private void insertMember(Long roomId, Long userId) {
        ChatRoomMemberId pk = new ChatRoomMemberId(roomId, userId);

        if (chatRoomMemberRepository.existsById(pk)) return;

        ChatRoomMember m = new ChatRoomMember();
        m.setId(pk);
        m.setJoinedAt(LocalDateTime.now());
        chatRoomMemberRepository.save(m);
    }


    private String makeDirectKey(Long a, Long b) {
        long x = Math.min(a, b);
        long y = Math.max(a, b);
        return x + "_" + y;
    }
    @Transactional
    public void leaveRoom(Long meId, Long roomId) {
        // 1) 내가 그 방 멤버인지 확인 (복합키: id.roomId / id.userId)
        ChatRoomMember member = chatRoomMemberRepository
                .findByIdRoomIdAndIdUserId(roomId, meId)
                .orElseThrow(() -> new RuntimeException("Not a member"));

        // 2) 방에서 나가기(멤버 row 삭제)
        chatRoomMemberRepository.delete(member);

        // 3) (선택) 방에 남은 멤버가 0명이면 방/메시지 정리
        long remain = chatRoomMemberRepository.countByIdRoomId(roomId);
        if (remain == 0) {
            chatRoomRepository.deleteById(roomId);
        }
    }


}

