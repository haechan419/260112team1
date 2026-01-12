package com.Team1_Back.service;

import com.Team1_Back.dto.ChatRoomListItemResponse;
import com.Team1_Back.repository.ChatRoomRepository;
import com.Team1_Back.repository.projection.ChatRoomListRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatRoomQueryService {

    private final ChatRoomRepository chatRoomRepository;

    public List<ChatRoomListItemResponse> myRooms(Long meId) {
        List<ChatRoomListRow> rows = chatRoomRepository.findMyRoomList(meId);

        return rows.stream().map(r -> new ChatRoomListItemResponse(
                r.getRoomId(),
                r.getType(),
                r.getDirectKey(),

                // ✅ 추가
                r.getPartnerName(),

                r.getLastMessageId(),
                r.getLastSenderId(),
                r.getLastContent(),
                r.getLastCreatedAt() != null
                        ? r.getLastCreatedAt()
                        .atZone(ZoneId.of("Asia/Seoul"))
                        .toInstant()
                        : null,

                r.getUnreadCount() != null ? r.getUnreadCount() : 0L
        )).toList();

    }
}
