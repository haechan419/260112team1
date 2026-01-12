package com.Team1_Back.domain;



import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Getter
@EqualsAndHashCode
@NoArgsConstructor
public class ChatRoomMemberId implements Serializable {
    private Long roomId;
    private Long userId;

    public ChatRoomMemberId(Long roomId, Long userId) {
        this.roomId = roomId;
        this.userId = userId;
    }
}
