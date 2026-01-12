package com.Team1_Back.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileImageDTO {

    private Long id;
    private Long userId;
    private String fileName;
    private String originalName;
    private Long fileSize;
    
    // 이미지 URL (프론트에서 사용)
    private String imageUrl;
    private String thumbnailUrl;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
