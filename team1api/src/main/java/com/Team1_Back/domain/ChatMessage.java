package com.Team1_Back.domain;


import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE) // ✅ Builder 안정화용
@Builder
@Getter
@Setter
@Entity
@Table(name = "chat_message", indexes = {
                @Index(name = "idx_cm_room_id_id", columnList = "room_id,id"),
                @Index(name = "idx_cm_room_created", columnList = "room_id,created_at")
})
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    // ✅ 파일만 보내는 메시지도 있으니 nullable=true 권장
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) this.createdAt = Instant.now();
    }
}
