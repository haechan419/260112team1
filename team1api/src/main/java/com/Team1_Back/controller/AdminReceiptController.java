package com.Team1_Back.controller;

import com.Team1_Back.dto.PageRequestDTO;
import com.Team1_Back.dto.PageResponseDTO;
import com.Team1_Back.dto.ReceiptDTO;
import com.Team1_Back.dto.ReceiptVerificationDTO;
import com.Team1_Back.dto.UserDTO;
import com.Team1_Back.service.AdminReceiptService;
import com.Team1_Back.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 관리자용 영수증 관리 REST API 컨트롤러
 * 
 * <p>관리자가 모든 사용자의 영수증을 조회하고 검증하는 기능을 제공합니다.
 * 모든 엔드포인트는 관리자 권한이 필요합니다.
 * 
 * @author Team1
 * @since 1.0
 */
@RestController
@RequestMapping("/api/admin/receipts")
@Slf4j
@RequiredArgsConstructor
public class AdminReceiptController {

    private final AdminReceiptService adminReceiptService;
    private final UserService userService;

    /**
     * 영수증 목록을 조회합니다 (관리자 전용).
     * 
     * <p>승인 상태와 승인자 ID로 필터링이 가능합니다.
     * 
     * @param status 필터링할 승인 상태 (선택)
     * @param approverId 승인자 ID로 필터링 (선택)
     * @param pageRequestDTO 페이지네이션 정보
     * @param principal 인증된 사용자 정보
     * @return 페이지네이션된 영수증 목록
     * @throws RuntimeException 관리자 권한이 없는 경우
     */
    @GetMapping("/list")
    public PageResponseDTO<ReceiptDTO> getList(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "approverId", required = false) Long approverId,
            PageRequestDTO pageRequestDTO,
            @AuthenticationPrincipal UserDTO principal) {
        
        if (principal == null) {
            throw new RuntimeException("인증이 필요합니다.");
        }
        
        Long userId = principal.getId();
        
        // 관리자 권한 체크
        if (!userService.isAdmin(userId)) {
            log.warn("관리자 권한이 없습니다. userId: " + userId);
            throw new RuntimeException("관리자 권한이 필요합니다.");
        }
        
        log.info("관리자 영수증 목록 조회 요청 - status: " + status + ", approverId: " + approverId);
        PageResponseDTO<ReceiptDTO> response = adminReceiptService.getList(pageRequestDTO, status, approverId);
        log.info("관리자 영수증 목록 조회 결과 - 총 " + response.getContent().size() + "건");
        return response;
    }

    /**
     * 특정 영수증의 상세 정보를 조회합니다 (관리자 전용).
     * 
     * @param id 조회할 영수증 ID
     * @param principal 인증된 사용자 정보
     * @return 영수증 상세 정보
     * @throws RuntimeException 관리자 권한이 없는 경우
     */
    @GetMapping("/{id}")
    public ReceiptDTO get(
            @PathVariable(name="id") Long id,
            @AuthenticationPrincipal UserDTO principal) {
        
        if (principal == null) {
            throw new RuntimeException("인증이 필요합니다.");
        }
        
        Long userId = principal.getId();
        
        // 관리자 권한 체크
        if (!userService.isAdmin(userId)) {
            log.warn("관리자 권한이 없습니다. userId: " + userId);
            throw new RuntimeException("관리자 권한이 필요합니다.");
        }
        
        return adminReceiptService.get(id);
    }

    /**
     * 영수증 이미지를 조회합니다 (관리자 전용).
     * 
     * @param id 조회할 영수증 ID
     * @param principal 인증된 사용자 정보
     * @return 영수증 이미지 리소스
     * @throws RuntimeException 관리자 권한이 없는 경우
     */
    @GetMapping("/{id}/image")
    public ResponseEntity<Resource> getImage(
            @PathVariable(name="id") Long id,
            @AuthenticationPrincipal UserDTO principal) {
        
        if (principal == null) {
            throw new RuntimeException("인증이 필요합니다.");
        }
        
        Long userId = principal.getId();
        
        // 관리자 권한 체크
        if (!userService.isAdmin(userId)) {
            log.warn("관리자 권한이 없습니다. userId: " + userId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        Resource resource = adminReceiptService.getImage(id);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    /**
     * 영수증에서 추출된 정보를 조회합니다 (관리자 전용).
     * 
     * <p>AI로 추출된 영수증 정보(가맹점, 금액, 카테고리 등)를 반환합니다.
     * 
     * @param id 조회할 영수증 ID
     * @param principal 인증된 사용자 정보
     * @return 영수증 추출 정보
     * @throws RuntimeException 관리자 권한이 없는 경우
     */
    @GetMapping("/{id}/extraction")
    public com.Team1_Back.dto.ReceiptExtractionDTO getExtraction(
            @PathVariable(name="id") Long id,
            @AuthenticationPrincipal UserDTO principal) {
        
        if (principal == null) {
            throw new RuntimeException("인증이 필요합니다.");
        }
        
        Long userId = principal.getId();
        
        // 관리자 권한 체크
        if (!userService.isAdmin(userId)) {
            log.warn("관리자 권한이 없습니다. userId: " + userId);
            throw new RuntimeException("관리자 권한이 필요합니다.");
        }
        
        return adminReceiptService.getExtraction(id);
    }

    /**
     * 영수증을 검증합니다 (관리자 전용).
     * 
     * <p>관리자가 영수증 정보를 검증하고 승인/거절 처리를 합니다.
     * 
     * @param id 검증할 영수증 ID (선택, verificationDTO에 포함될 수 있음)
     * @param verificationDTO 검증 정보 (검증된 가맹점, 금액, 카테고리, 사유 등)
     * @param principal 인증된 사용자 정보
     * @return 검증 결과를 포함한 Map
     * @throws RuntimeException 관리자 권한이 없는 경우
     */
    @PutMapping("/{id}/verify")
    public Map<String, String> verify(
            @PathVariable(name="id", required = false) Long id,
            @RequestBody ReceiptVerificationDTO verificationDTO,
            @AuthenticationPrincipal UserDTO principal) {
        
        if (principal == null) {
            throw new RuntimeException("인증이 필요합니다.");
        }
        
        Long adminId = principal.getId();
        
        // 관리자 권한 체크
        if (!userService.isAdmin(adminId)) {
            log.warn("관리자 권한이 없습니다. userId: " + adminId);
            throw new RuntimeException("관리자 권한이 필요합니다.");
        }
        
        adminReceiptService.verify(id, verificationDTO, adminId);
        return Map.of("RESULT", "SUCCESS");
    }
    
    /**
     * 지출 내역 ID로 영수증을 검증합니다 (관리자 전용).
     * 
     * <p>영수증이 아직 업로드되지 않은 지출 내역에 대해 검증을 수행합니다.
     * 
     * @param expenseId 검증할 지출 내역 ID
     * @param verificationDTO 검증 정보
     * @param principal 인증된 사용자 정보
     * @return 검증 결과를 포함한 Map
     * @throws RuntimeException 관리자 권한이 없는 경우
     */
    @PutMapping("/expense/{expenseId}/verify")
    public Map<String, String> verifyByExpenseId(
            @PathVariable(name="expenseId") Long expenseId,
            @RequestBody ReceiptVerificationDTO verificationDTO,
            @AuthenticationPrincipal UserDTO principal) {
        
        if (principal == null) {
            throw new RuntimeException("인증이 필요합니다.");
        }
        
        Long adminId = principal.getId();
        
        // 관리자 권한 체크
        if (!userService.isAdmin(adminId)) {
            log.warn("관리자 권한이 없습니다. userId: " + adminId);
            throw new RuntimeException("관리자 권한이 필요합니다.");
        }
        
        verificationDTO.setExpenseId(expenseId);
        adminReceiptService.verify(null, verificationDTO, adminId);
        return Map.of("RESULT", "SUCCESS");
    }
}

