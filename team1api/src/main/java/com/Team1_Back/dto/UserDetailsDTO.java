package com.Team1_Back.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDetailsDTO {

    private Long id;
    private String employeeNo;
    private String name;
    private String email;
    private LocalDate birthDate;
    private String phone;
    private String address;
    private String addressDetail;
    private String departmentName;
    private String positionName;
    private String role; // 권한 (USER, ADMIN)
    private boolean isLocked;
    private boolean isActive;
    private LocalDateTime createdUserAt;
    private LocalDateTime updatedUserAt;
    
    // 프로필 이미지 URL 추가
    private String profileImageUrl;
    private String thumbnailUrl;
}
