package com.Team1_Back.domain;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name="chat_room")
@Getter @Setter
public class ChatRoom {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length=20, nullable=false)
    private String type; // DM/GROUP

    @Column(name="direct_key", length=50) // nullable=true가 기본
    private String directKey;

    @Column(name="created_at")
    private LocalDateTime createdAt;
}
