package com.Team1_Back.service;

import com.Team1_Back.domain.ChatMessage;
import com.Team1_Back.domain.ChatRoom;
import com.Team1_Back.domain.ChatRoomMember;
import com.Team1_Back.domain.ChatRoomMemberId;
import com.Team1_Back.dto.ChatAttachmentDto;
import com.Team1_Back.dto.ChatMessageResponse;
import com.Team1_Back.dto.ChatRoomMetaResponse;
import com.Team1_Back.repository.ChatMessageRepository;
import com.Team1_Back.repository.ChatRoomMemberRepository;
import com.Team1_Back.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ChatService {
    private final SimpMessagingTemplate messagingTemplate;

    private final ChatRoomRepository roomRepo;
    private final ChatRoomMemberRepository memberRepo;
    private final ChatMessageRepository messageRepo;
    private  final ChatRoomSecurityService chatRoomSecurityService;
    private final ChatMessageRepository chatMessageRepository;

    private final ChatRoomMemberRepository chatRoomMemberRepository;

    public static String directKey(Long a, Long b) {
        long min = Math.min(a, b);
        long max = Math.max(a, b);
        return min + "_" + max;
    }

    // =========================
    // DIRECT ROOM (DM)
    // =========================
    @Transactional
    public Long getOrCreateDirectRoom(Long meId, Long targetId) {
        if (meId.equals(targetId)) throw new IllegalArgumentException("Cannot chat with self");

        String key = directKey(meId, targetId);

        ChatRoom room = roomRepo.findByDirectKey(key).orElseGet(() -> {
            ChatRoom r = new ChatRoom();
            r.setType("DIRECT");     // 너 규칙 유지
            r.setDirectKey(key);
            r.setCreatedAt(LocalDateTime.now());
            return roomRepo.save(r);
        });

        // ✅ EmbeddedId 방식으로 멤버 insert
        insertMemberIfAbsent(room.getId(), meId);
        insertMemberIfAbsent(room.getId(), targetId);

        return room.getId();
    }

    private void insertMemberIfAbsent(Long roomId, Long userId) {
        ChatRoomMemberId pk = new ChatRoomMemberId(roomId, userId);

        if (memberRepo.existsById(pk)) return;

        ChatRoomMember m = new ChatRoomMember();
        m.setId(pk);
        m.setJoinedAt(LocalDateTime.now()); // ✅ 엔티티가 LocalDateTime
        memberRepo.save(m);
    }

    // =========================
    // MEMBER CHECK
    // =========================
    public void assertMember(Long roomId, Long userId) {
        // ✅ Repository가 이 형태로 있어야 함
        if (!memberRepo.existsByIdRoomIdAndIdUserId(roomId, userId)) {
            throw new IllegalStateException("Not a room member");
        }
    }

    // =========================
    // SEND MESSAGE
    // =========================
    @Transactional
    public ChatMessageResponse sendMessage(Long roomId, Long senderId, String content) {
        if (content == null || content.trim().isEmpty()) throw new IllegalArgumentException("Empty content");

        assertMember(roomId, senderId);

        ChatMessage msg = new ChatMessage();
        msg.setRoomId(roomId);
        msg.setSenderId(senderId);
        msg.setContent(content.trim());
        msg.setCreatedAt(Instant.now()); // ChatMessage는 Instant 유지 OK

        ChatMessage saved = messageRepo.save(msg);

        final Long savedId = saved.getId();

        memberRepo.findByIdRoomIdAndIdUserId(roomId, senderId).ifPresent(m -> {
            m.setLastReadMessageId(savedId);
            m.setLastReadAt(LocalDateTime.now()); // ✅ 엔티티가 LocalDateTime
            memberRepo.save(m);
        });

        return new ChatMessageResponse(savedId, roomId, senderId, saved.getContent(), saved.getCreatedAt(),List.of() );
    }

    public void broadcastRoomsChanged(Long roomId) {
        List<Long> userIds = chatRoomMemberRepository.findUserIdsByRoomId(roomId);

        for (Long uid : userIds) {
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(uid),
                    "/queue/rooms",
                    Map.of("type", "ROOMS_CHANGED")
            );
        }
    }

    // =========================
    // ROOM META
    // =========================
    @Transactional(readOnly = true)
    public ChatRoomMetaResponse getRoomMeta(Long roomId, Long meId) {
        assertMember(roomId, meId);

        List<ChatRoomMember> members = memberRepo.findAllByIdRoomId(roomId);

        Long meLast = null;
        Long otherLast = null;

        Map<Long, Long> map = new java.util.HashMap<>();

        for (ChatRoomMember m : members) {
            Long uid = m.getId().getUserId();
            Long last = m.getLastReadMessageId(); // null 가능

            map.put(uid, last);

            if (uid.equals(meId)) meLast = last;
        }

        return new ChatRoomMetaResponse(
                roomId,
                meLast,
                map,
                members.size()
        );
    }

    // =========================
    // MESSAGES PAGE
    // =========================
    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getMessages(Long roomId, Long meId, Long cursor, int limit) {
        assertMember(roomId, meId);

        int size = Math.max(1, Math.min(limit, 50));

        // 1) 페이지에 해당하는 messageId만 먼저 가져오기
        List<Long> ids = messageRepo.findPageIds(roomId, cursor, size);
        if (ids.isEmpty()) return List.of();

        // 2) attachments까지 join fetch로 가져오기
        List<ChatMessage> list = messageRepo.findByIdInWithAttachments(ids);

        // 3) IN 조회는 순서 보장 X → id desc로 정렬 맞추기
        list.sort((a, b) -> Long.compare(b.getId(), a.getId()));

        // 4) 반드시 toResponse로 attachments 포함 변환
        return list.stream().map(this::toResponse).toList();
    }

    // =========================
    // READ UPDATE
    // =========================
    @Transactional
    public void updateRead(Long roomId, Long meId, Long lastReadMessageId) {
        ChatRoomMember m = memberRepo.findByIdRoomIdAndIdUserId(roomId, meId)
                .orElseThrow(() -> new IllegalStateException("Not a room member"));

        // lastReadMessageId가 null이면: 메시지 없는 방이거나, 프론트가 안 보냈거나
        if (lastReadMessageId == null) {
            m.setLastReadAt(LocalDateTime.now());
            return;
        }

        Long current = m.getLastReadMessageId();

        // 읽음은 뒤로 가면 안 됨: max(current, incoming)
        if (current == null || lastReadMessageId > current) {
            m.setLastReadMessageId(lastReadMessageId);
        }
        m.setLastReadAt(LocalDateTime.now());

        // ✅ JPA면 save() 없어도 됨 (m이 영속 상태)
        // memberRepo.save(m);
    }
    private ChatMessageResponse toResponse(ChatMessage m) {
        return ChatMessageResponse.builder()
                .messageId(m.getId())
                .roomId(m.getRoomId())
                .senderId(m.getSenderId())
                .content(m.getContent())
                .createdAt(m.getCreatedAt())
                .attachments(
                        m.getAttachments() == null
                                ? List.of()
                                : m.getAttachments().stream()
                                .map(att -> ChatAttachmentDto.builder()
                                        .attachmentId(att.getId())
                                        .originalName(att.getOriginalName())
                                        .mimeType(att.getMimeType())
                                        .size(att.getFileSize())
//                                        .url(att.getFileUrl())
                                        .build()
                                )
                                .toList()
                )
                .build();
    }


    @Transactional
    public Long createGroupRoom(Long meId, List<Long> memberUserIds) {
        ChatRoom room = new ChatRoom();
        room.setType("GROUP");
        room.setDirectKey(null);
        room = roomRepo.save(room);

        List<Long> all = new java.util.ArrayList<>();
        all.add(meId);
        if (memberUserIds != null) all.addAll(memberUserIds);

        for (Long uid : all.stream().distinct().toList()) {
            insertMemberIfAbsent(room.getId(), uid);
        }

        return room.getId();
    }


    @Transactional
    public void invite(Long roomId, Long meId, List<Long> userIds) {
        assertMember(roomId, meId);

        if (userIds == null || userIds.isEmpty()) return;

        for (Long uid : userIds.stream().distinct().toList()) {
            insertMemberIfAbsent(roomId, uid);
        }
    }

    @Transactional
    public List<ChatMessageResponse> getMessages(Long roomId, Long meId, int limit) {
        chatRoomSecurityService.assertMember(meId, roomId);

        List<Long> ids = chatMessageRepository.findRecentIds(roomId, limit);
        if (ids.isEmpty()) return List.of();

        List<ChatMessage> list = chatMessageRepository.findByIdInWithAttachments(ids);

        return list.stream()
                .map(this::toResponse)
                .toList();
    }




}
