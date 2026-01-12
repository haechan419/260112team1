package com.Team1_Back.service;

import com.Team1_Back.domain.User;
import com.Team1_Back.domain.UserProfileImage;
import com.Team1_Back.dto.UserProfileImageDTO;
import com.Team1_Back.repository.UserProfileImageRepository;
import com.Team1_Back.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserProfileImageServiceImpl implements UserProfileImageService {

    private final UserProfileImageRepository profileImageRepository;
    private final UserRepository userRepository;

    @Value("${com.team1.upload.path}")
    private String uploadPath;

    // 허용된 확장자
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png");
    
    // 최대 파일 크기 (5MB)
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
    
    // 썸네일 크기
    private static final int THUMBNAIL_WIDTH = 310;
    private static final int THUMBNAIL_HEIGHT = 280;

    @Override
    public UserProfileImageDTO uploadProfileImage(Long userId, MultipartFile file) {
        log.info("프로필 이미지 업로드 시작 - userId: {}", userId);

        // 1. 파일 검증
        validateImageFile(file);

        // 2. 사용자 존재 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + userId));

        // 3. 기존 이미지가 있으면 삭제 (트랜잭션 내에서 즉시 반영)
        profileImageRepository.findByUserId(userId).ifPresent(existingImage -> {
            // 파일 삭제
            deleteFile(existingImage.getFileName());
            // DB 삭제
            profileImageRepository.delete(existingImage);
            profileImageRepository.flush(); // ✅ 즉시 DB에 반영하여 UNIQUE 제약 조건 해제
            log.info("기존 프로필 이미지 삭제 완료 - fileName: {}", existingImage.getFileName());
        });

        // 4. 파일 저장
        String savedFileName = saveFile(file);

        // 5. DB 저장
        UserProfileImage profileImage = UserProfileImage.builder()
                .user(user)
                .fileName(savedFileName)
                .originalName(file.getOriginalFilename())
                .fileSize(file.getSize())
                .build();

        UserProfileImage saved = profileImageRepository.save(profileImage);
        log.info("프로필 이미지 저장 완료 - fileName: {}", savedFileName);

        return toDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileImageDTO getProfileImage(Long userId) {
        return profileImageRepository.findByUserId(userId)
                .map(this::toDTO)
                .orElse(null);
    }

    @Override
    public void deleteProfileImage(Long userId) {
        log.info("프로필 이미지 삭제 시작 - userId: {}", userId);

        profileImageRepository.findByUserId(userId).ifPresent(image -> {
            // 파일 삭제
            deleteFile(image.getFileName());
            // DB 삭제
            profileImageRepository.delete(image);
            log.info("프로필 이미지 삭제 완료 - fileName: {}", image.getFileName());
        });
    }

    @Override
    @Transactional(readOnly = true)
    public String getThumbnailUrl(Long userId) {
        return profileImageRepository.findFileNameByUserId(userId)
                .map(fileName -> "/api/view/user_image/s_" + fileName)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasProfileImage(Long userId) {
        return profileImageRepository.existsByUserId(userId);
    }

    // ═══════════════════════════════════════════════════════════════
    // Private Helper Methods
    // ═══════════════════════════════════════════════════════════════

    /**
     * 이미지 파일 검증
     */
    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("파일이 비어있습니다.");
        }

        // 파일 크기 검증
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new RuntimeException("파일 크기는 5MB를 초과할 수 없습니다.");
        }

        // 확장자 검증
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new RuntimeException("파일 확장자를 확인할 수 없습니다.");
        }

        String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new RuntimeException("허용되지 않은 파일 형식입니다. (jpg, jpeg, png만 가능)");
        }

        // Content-Type 검증
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new RuntimeException("이미지 파일만 업로드할 수 있습니다.");
        }
    }

    /**
     * 파일 저장 (원본 + 썸네일)
     */
    private String saveFile(MultipartFile file) {
        try {
            // 프로필 이미지 저장 경로
            Path profilePath = Paths.get(uploadPath, "user_image");
            Files.createDirectories(profilePath);

            // 파일명 생성
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String savedFileName = UUID.randomUUID().toString() + extension;

            // 원본 파일 저장
            Path filePath = profilePath.resolve(savedFileName);
            Files.copy(file.getInputStream(), filePath);

            // 썸네일 생성 (310x280)
            Path thumbnailPath = profilePath.resolve("s_" + savedFileName);
            Thumbnails.of(filePath.toFile())
                    .size(THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT)
                    .toFile(thumbnailPath.toFile());

            log.info("파일 저장 완료 - 원본: {}, 썸네일: s_{}", savedFileName, savedFileName);
            return savedFileName;

        } catch (IOException e) {
            log.error("파일 저장 실패: {}", e.getMessage());
            throw new RuntimeException("파일 저장에 실패했습니다.", e);
        }
    }

    /**
     * 파일 삭제 (원본 + 썸네일)
     */
    private void deleteFile(String fileName) {
        try {
            Path profilePath = Paths.get(uploadPath, "user_image");
            
            // 원본 삭제
            Path filePath = profilePath.resolve(fileName);
            Files.deleteIfExists(filePath);

            // 썸네일 삭제
            Path thumbnailPath = profilePath.resolve("s_" + fileName);
            Files.deleteIfExists(thumbnailPath);

            log.info("파일 삭제 완료 - {}", fileName);
        } catch (IOException e) {
            log.error("파일 삭제 실패: {}", e.getMessage());
        }
    }

    /**
     * Entity → DTO 변환
     */
    private UserProfileImageDTO toDTO(UserProfileImage entity) {
        return UserProfileImageDTO.builder()
                .id(entity.getId())
                .userId(entity.getUser().getId())
                .fileName(entity.getFileName())
                .originalName(entity.getOriginalName())
                .fileSize(entity.getFileSize())
                .imageUrl("/api/view/user_image/" + entity.getFileName())
                .thumbnailUrl("/api/view/user_image/s_" + entity.getFileName())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
