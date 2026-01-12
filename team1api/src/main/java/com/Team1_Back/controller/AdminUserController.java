package com.Team1_Back.controller;

import com.Team1_Back.dto.*;
import com.Team1_Back.service.AdminUserService;
import com.Team1_Back.service.UserProfileImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final UserProfileImageService profileImageService;

    // 사원 목록 조회
    // GET /api/admin/users?page=1&size=10&searchType=name&keyword=홍길동&department=개발1팀
    @GetMapping
    public ResponseEntity<PageResponseDTO<UserListDTO>> getUsers(PageRequestDTO request) {
        log.info("사원 목록 조회 요청: {}", request);
        PageResponseDTO<UserListDTO> response = adminUserService.getUsers(request);
        return ResponseEntity.ok(response);
    }

    // 사원 상세 조회
    // GET /api/admin/users/1
    @GetMapping("/{id}")
    public ResponseEntity<UserDetailsDTO> getUser(@PathVariable Long id) {
        log.info("사원 상세 조회 요청: id={}", id);
        UserDetailsDTO response = adminUserService.getUser(id);
        return ResponseEntity.ok(response);
    }

    // 사원 등록
    // POST /api/admin/users
    @PostMapping
    public ResponseEntity<Map<String, Long>> createUser(@RequestBody UserCreateDTO dto) {
        log.info("사원 등록 요청: {}", dto.getEmployeeNo());
        Long id = adminUserService.createUser(dto);
        return ResponseEntity.ok(Map.of("id", id));
    }

    // 사원 수정
    // PUT /api/admin/users/1
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, String>> updateUser(
            @PathVariable Long id,
            @RequestBody UserUpdateDTO dto) {
        log.info("사원 수정 요청: id={}", id);
        adminUserService.updateUser(id, dto);
        return ResponseEntity.ok(Map.of("result", "success"));
    }

    // 퇴사 처리
    // PUT /api/admin/users/1/resign
    @PutMapping("/{id}/resign")
    public ResponseEntity<Map<String, String>> resignUser(@PathVariable Long id) {
        log.info("퇴사 처리 요청: id={}", id);
        adminUserService.resignUser(id);
        return ResponseEntity.ok(Map.of("result", "success"));
    }

    // 계정 잠금 해제
    // PUT /api/admin/users/1/unlock
    @PutMapping("/{id}/unlock")
    public ResponseEntity<Map<String, String>> unlockUser(@PathVariable Long id) {
        log.info("계정 잠금 해제 요청: id={}", id);
        adminUserService.unlockUser(id);
        return ResponseEntity.ok(Map.of("result", "success"));
    }

    // ═══════════════════════════════════════════════════════════════
    // 프로필 이미지 관련 API
    // ═══════════════════════════════════════════════════════════════

    // 프로필 이미지 업로드
    // POST /api/admin/users/1/profile-image
    @PostMapping("/{id}/profile-image")
    public ResponseEntity<UserProfileImageDTO> uploadProfileImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        log.info("프로필 이미지 업로드 요청: userId={}, fileName={}", id, file.getOriginalFilename());
        UserProfileImageDTO result = profileImageService.uploadProfileImage(id, file);
        return ResponseEntity.ok(result);
    }

    // 프로필 이미지 조회
    // GET /api/admin/users/1/profile-image
    @GetMapping("/{id}/profile-image")
    public ResponseEntity<UserProfileImageDTO> getProfileImage(@PathVariable Long id) {
        log.info("프로필 이미지 조회 요청: userId={}", id);
        UserProfileImageDTO result = profileImageService.getProfileImage(id);
        if (result == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(result);
    }

    // 프로필 이미지 삭제
    // DELETE /api/admin/users/1/profile-image
    @DeleteMapping("/{id}/profile-image")
    public ResponseEntity<Map<String, String>> deleteProfileImage(@PathVariable Long id) {
        log.info("프로필 이미지 삭제 요청: userId={}", id);
        profileImageService.deleteProfileImage(id);
        return ResponseEntity.ok(Map.of("result", "success"));
    }
}
