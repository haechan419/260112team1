package com.Team1_Back.domain;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name="chat_room_member")
@Getter @Setter
public class ChatRoomMember {

    @EmbeddedId
    private ChatRoomMemberId id;

    @Column(name="last_read_message_id")
    private Long lastReadMessageId;

    @Column(name="last_read_at")
    private LocalDateTime lastReadAt;

    @Column(name="joined_at")
    private LocalDateTime joinedAt;
}

