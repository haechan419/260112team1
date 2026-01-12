package com.Team1_Back.repository;

import com.Team1_Back.domain.UserProfileImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserProfileImageRepository extends JpaRepository<UserProfileImage, Long> {

    // 사용자 ID로 프로필 이미지 조회
    Optional<UserProfileImage> findByUserId(Long userId);

    // 사용자 ID로 프로필 이미지 존재 여부 확인
    boolean existsByUserId(Long userId);

    // 사용자 ID로 프로필 이미지 삭제
    void deleteByUserId(Long userId);

    // 사용자 ID로 파일명만 조회 (성능 최적화)
    @Query("SELECT p.fileName FROM UserProfileImage p WHERE p.user.id = :userId")
    Optional<String> findFileNameByUserId(@Param("userId") Long userId);
}
