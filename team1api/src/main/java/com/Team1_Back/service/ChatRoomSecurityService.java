package com.Team1_Back.service;

import com.Team1_Back.repository.ChatRoomMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ChatRoomSecurityService {

    private final ChatRoomMemberRepository chatRoomMemberRepository;

    public boolean isMember(Long userId, Long roomId) {
        return chatRoomMemberRepository.existsByIdRoomIdAndIdUserId(roomId, userId);
    }
    public void assertMember(Long userId, Long roomId) {
        if (!isMember(userId, roomId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "not a member");
        }
    }

}
