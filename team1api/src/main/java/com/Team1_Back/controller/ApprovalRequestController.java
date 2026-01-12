package com.Team1_Back.controller;

import com.Team1_Back.dto.*;
import com.Team1_Back.dto.UserDTO;
import com.Team1_Back.service.ApprovalService;
import com.Team1_Back.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 승인 요청 관리를 위한 REST API 컨트롤러
 * 
 * <p>지출 내역 등의 승인 요청을 조회하고 관리하는 기능을 제공합니다.
 * 일반 사용자는 본인의 승인 요청만 조회할 수 있으며, 관리자는 모든 승인 요청을 조회할 수 있습니다.
 * 
 * @author Team1
 * @since 1.0
 */
@RestController
@RequestMapping("/api/approval-requests")
@Slf4j
@RequiredArgsConstructor
public class ApprovalRequestController {

    private final ApprovalService approvalService;
    private final UserService userService;

    /**
     * 승인 요청 목록을 조회합니다.
     * 
     * <p>일반 사용자는 본인의 승인 요청만 조회하며, 관리자는 모든 승인 요청을 조회할 수 있습니다.
     * 요청 유형, 상태, 날짜 범위로 필터링이 가능합니다.
     * 
     * @param requestType 요청 유형 (EXPENSE, STORE_ORDER 등, 선택)
     * @param status 승인 상태 (선택)
     * @param startDate 조회 시작 날짜 (ISO DATE 형식, 선택)
     * @param endDate 조회 종료 날짜 (ISO DATE 형식, 선택)
     * @param pageRequestDTO 페이지네이션 정보
     * @param principal 인증된 사용자 정보
     * @return 페이지네이션된 승인 요청 목록
     */
    @GetMapping("/list")
    public PageResponseDTO<ApprovalRequestDTO> getList(
            @RequestParam(value = "requestType", required = false) String requestType,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            PageRequestDTO pageRequestDTO,
            @AuthenticationPrincipal UserDTO principal) {
        
        if (principal == null) {
            throw new RuntimeException("인증이 필요합니다.");
        }
        
        Long userId = principal.getId();
        boolean isAdmin = userService.isAdmin(userId);

        return approvalService.getList(userId, isAdmin, pageRequestDTO, requestType, status, startDate, endDate);
    }

    /**
     * 특정 승인 요청의 상세 정보를 조회합니다.
     * 
     * <p>일반 사용자는 본인의 승인 요청만 조회할 수 있으며, 관리자는 모든 승인 요청을 조회할 수 있습니다.
     * 
     * @param id 조회할 승인 요청 ID
     * @param principal 인증된 사용자 정보
     * @return 승인 요청 상세 정보
     */
    @GetMapping("/{id}")
    public ApprovalRequestDTO get(
            @PathVariable(name="id") Long id,
            @AuthenticationPrincipal UserDTO principal) {
        
        if (principal == null) {
            throw new RuntimeException("인증이 필요합니다.");
        }
        
        Long userId = principal.getId();
        boolean isAdmin = userService.isAdmin(userId);

        return approvalService.get(id, userId, isAdmin);
    }

    /**
     * 승인 요청의 처리 이력을 조회합니다.
     * 
     * <p>승인, 거절, 추가 정보 요청 등의 처리 이력을 반환합니다.
     * 
     * @param id 조회할 승인 요청 ID
     * @param principal 인증된 사용자 정보
     * @return 승인 처리 이력 목록
     */
    @GetMapping("/{id}/logs")
    public List<ApprovalActionLogDTO> getLogs(
            @PathVariable(name="id") Long id,
            @AuthenticationPrincipal UserDTO principal) {
        
        if (principal == null) {
            throw new RuntimeException("인증이 필요합니다.");
        }
        
        Long userId = principal.getId();
        boolean isAdmin = userService.isAdmin(userId);

        return approvalService.getLogs(id, userId, isAdmin);
    }

    /**
     * 승인 요청에 대한 처리 액션을 수행합니다 (관리자 전용).
     * 
     * <p>관리자가 승인 요청을 승인, 거절, 추가 정보 요청 등의 액션으로 처리합니다.
     * 
     * @param id 처리할 승인 요청 ID
     * @param actionDTO 처리 액션 정보 (액션 타입, 메모 등)
     * @param principal 인증된 사용자 정보
     * @return 처리 결과를 포함한 Map
     * @throws RuntimeException 관리자 권한이 없는 경우
     */
    @PutMapping("/{id}/action")
    public java.util.Map<String, String> action(
            @PathVariable(name="id") Long id,
            @RequestBody ApprovalActionDTO actionDTO,
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
        
        approvalService.action(id, actionDTO, adminId);
        return java.util.Map.of("RESULT", "SUCCESS");
    }

    /**
     * 지출 결재 목록을 조회합니다 (관리자 전용).
     * 
     * <p>지출 내역 승인 요청만 필터링하여 조회합니다.
     * 하위 경로를 사용하여 경로 충돌을 방지합니다.
     * 
     * @param status 승인 상태 (선택)
     * @param startDate 조회 시작 날짜 (ISO DATE 형식, 선택)
     * @param endDate 조회 종료 날짜 (ISO DATE 형식, 선택)
     * @param pageRequestDTO 페이지네이션 정보
     * @param principal 인증된 사용자 정보
     * @return 페이지네이션된 지출 결재 목록
     * @throws RuntimeException 관리자 권한이 없는 경우
     */
    @GetMapping("/types/expense")
    public PageResponseDTO<ApprovalRequestDTO> getExpenseList(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            PageRequestDTO pageRequestDTO,
            @AuthenticationPrincipal UserDTO principal) {
        
        if (principal == null) {
            throw new RuntimeException("인증이 필요합니다.");
        }
        
        Long userId = principal.getId();
        
        // 관리자 전용 API이므로 권한 체크
        if (!userService.isAdmin(userId)) {
            log.warn("관리자 권한이 없습니다. userId: " + userId);
            throw new RuntimeException("관리자 권한이 필요합니다.");
        }

        boolean isAdmin = userService.isAdmin(userId);
        return approvalService.getList(userId, isAdmin, pageRequestDTO, "EXPENSE", status, startDate, endDate);
    }

}

