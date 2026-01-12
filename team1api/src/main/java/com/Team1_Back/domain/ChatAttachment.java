package com.Team1_Back.domain;

// package com.Team1_Back.chat.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_attachment",
        indexes = {
                @Index(name = "idx_chat_attachment_message", columnList = "message_id"),
                @Index(name = "idx_chat_attachment_room", columnList = "room_id"),
                @Index(name = "idx_chat_attachment_uploader", columnList = "uploader_id")
        })
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // FK: message_id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private ChatMessage message;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "uploader_id", nullable = false)
    private Long uploaderId;

    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    @Column(name = "stored_name", nullable = false, length = 255)
    private String storedName;

    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "storage_type", nullable = false, length = 20)
    private String storageType; // LOCAL / S3 ...

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;    // local path or s3 key

    @Column(name = "file_url", nullable = false, length = 500)
    private String fileUrl;     // download endpoint

    @CreationTimestamp
    @Column(name="created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name="deleted_at")
    private LocalDateTime deletedAt;
}
