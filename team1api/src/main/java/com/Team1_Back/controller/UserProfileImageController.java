package com.Team1_Back.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 프로필 이미지 조회 컨트롤러
 * - 인증 없이 이미지 파일 조회 가능 (공개 API)
 */
@RestController
@RequestMapping("/api/view")
@Slf4j
public class UserProfileImageController {

    @Value("${com.team1.upload.path}")
    private String uploadPath;

    /**
     * 프로필 이미지 조회
     * GET /api/view/user_image/{fileName}
     * GET /api/view/user_image/s_{fileName}  (썸네일)
     */
    @GetMapping("/user_image/{fileName}")
    public ResponseEntity<Resource> getProfileImage(@PathVariable String fileName) {
        log.info("프로필 이미지 조회 요청: {}", fileName);

        try {
            // 먼저 user_image 경로에서 찾기
            Path filePath = Paths.get(uploadPath, "user_image", fileName);
            Resource resource = new FileSystemResource(filePath);

            // user_image에 없으면 profile 경로에서 찾기 (하위 호환성)
            if (!resource.exists() || !resource.isReadable()) {
                filePath = Paths.get(uploadPath, "profile", fileName);
                resource = new FileSystemResource(filePath);
            }

            if (!resource.exists() || !resource.isReadable()) {
                log.warn("파일을 찾을 수 없음: {}", fileName);
                return ResponseEntity.notFound().build();
            }

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_TYPE, Files.probeContentType(filePath));

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);

        } catch (Exception e) {
            log.error("프로필 이미지 조회 실패: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 프로필 이미지 조회 (하위 호환성 - 이전 경로 지원)
     * GET /api/view/profile/{fileName}
     * GET /api/view/profile/s_{fileName}  (썸네일)
     */
    @GetMapping("/profile/{fileName}")
    public ResponseEntity<Resource> getProfileImageLegacy(@PathVariable String fileName) {
        log.info("프로필 이미지 조회 요청 (legacy): {}", fileName);

        try {
            // 먼저 profile 경로에서 찾기
            Path filePath = Paths.get(uploadPath, "profile", fileName);
            Resource resource = new FileSystemResource(filePath);

            // profile에 없으면 user_image 경로에서 찾기
            if (!resource.exists() || !resource.isReadable()) {
                filePath = Paths.get(uploadPath, "user_image", fileName);
                resource = new FileSystemResource(filePath);
            }

            if (!resource.exists() || !resource.isReadable()) {
                log.warn("파일을 찾을 수 없음: {}", fileName);
                return ResponseEntity.notFound().build();
            }

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_TYPE, Files.probeContentType(filePath));

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);

        } catch (Exception e) {
            log.error("프로필 이미지 조회 실패: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}
