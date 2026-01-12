package com.Team1_Back.service;

import com.Team1_Back.dto.UserProfileImageDTO;
import org.springframework.web.multipart.MultipartFile;

public interface UserProfileImageService {

    /**
     * 프로필 이미지 업로드
     * - 기존 이미지가 있으면 삭제 후 새로 업로드
     * - 원본 + 썸네일(310x280) 저장
     */
    UserProfileImageDTO uploadProfileImage(Long userId, MultipartFile file);

    /**
     * 프로필 이미지 조회
     */
    UserProfileImageDTO getProfileImage(Long userId);

    /**
     * 프로필 이미지 삭제
     */
    void deleteProfileImage(Long userId);

    /**
     * 썸네일 URL 조회 (로그인 시 사용)
     */
    String getThumbnailUrl(Long userId);

    /**
     * 프로필 이미지 존재 여부 확인
     */
    boolean hasProfileImage(Long userId);
}
