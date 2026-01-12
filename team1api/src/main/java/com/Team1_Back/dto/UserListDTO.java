package com.Team1_Back.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserListDTO {

    private Long id;
    private String employeeNo;
    private String name;
    private String departmentName;
    private String email;
    private String phone;
    private LocalDateTime createdUserAt;
    private boolean locked;
    private boolean active;
    
    // 프로필 이미지 썸네일 URL 추가
    private String thumbnailUrl;
}
