package com.Team1_Back.controller;

import com.Team1_Back.dto.DepartmentStatisticsDTO;
import com.Team1_Back.dto.UserDTO;
import com.Team1_Back.service.AccountingService;
import com.Team1_Back.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 회계 통계 관리를 위한 REST API 컨트롤러 (관리자 전용)
 * 
 * <p>
 * 부서별, 카테고리별 지출 통계 및 예산 관련 정보를 조회하는 기능을 제공합니다.
 * 모든 엔드포인트는 관리자 권한이 필요합니다.
 * 
 * @author Team1
 * @since 1.0
 */
@RestController
@RequestMapping("/api/admin/accounting")
@Log4j2
@RequiredArgsConstructor
public class AccountingController {

    private final AccountingService accountingService;
    private final UserService userService;

    /**
     * 부서별 지출 통계를 조회합니다 (관리자 전용).
     * 
     * <p>
     * 각 부서별 지출 금액과 건수를 집계하여 반환합니다.
     * 승인 상태로 필터링이 가능합니다.
     * 
     * @param status    필터링할 승인 상태 (선택)
     * @param principal 인증된 사용자 정보
     * @return 부서별 통계 목록
     * @throws RuntimeException 관리자 권한이 없는 경우
     */
    @GetMapping("/statistics/department")
    public List<DepartmentStatisticsDTO> getDepartmentStatistics(
            @RequestParam(value = "status", required = false) String status,
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

        log.info("부서별 통계 조회 요청 - userId: " + userId + ", status: " + status);
        return accountingService.getDepartmentStatistics(status);
    }

    /**
     * 부서 목록을 조회합니다 (관리자 전용).
     * 
     * @param principal 인증된 사용자 정보
     * @return 부서명 목록
     * @throws RuntimeException 관리자 권한이 없는 경우
     */
    @GetMapping("/departments")
    public List<String> getDepartments(@AuthenticationPrincipal UserDTO principal) {

        if (principal == null) {
            throw new RuntimeException("인증이 필요합니다.");
        }

        Long userId = principal.getId();

        // 관리자 권한 체크
        if (!userService.isAdmin(userId)) {
            log.warn("관리자 권한이 없습니다. userId: " + userId);
            throw new RuntimeException("관리자 권한이 필요합니다.");
        }

        log.info("부서 목록 조회 요청 - userId: " + userId);
        return accountingService.getDepartments();
    }

    /**
     * 카테고리별 지출 통계를 조회합니다 (관리자 전용).
     * 
     * <p>
     * 각 카테고리별 지출 금액과 건수를 집계하여 반환합니다.
     * 승인 상태로 필터링이 가능합니다.
     * 
     * @param status    필터링할 승인 상태 (선택)
     * @param principal 인증된 사용자 정보
     * @return 카테고리별 통계 목록
     * @throws RuntimeException 관리자 권한이 없는 경우
     */
    @GetMapping("/statistics/category")
    public List<Map<String, Object>> getCategoryStatistics(
            @RequestParam(value = "status", required = false) String status,
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

        log.info("카테고리별 통계 조회 요청 - userId: " + userId + ", status: " + status);
        return accountingService.getCategoryStatistics(status);
    }

    /**
     * 전체 통계 요약을 조회합니다 (관리자 전용).
     * 
     * <p>
     * 전체 지출 금액, 건수, 평균 금액 등의 요약 정보를 반환합니다.
     * 
     * @param principal 인증된 사용자 정보
     * @return 통계 요약 정보를 포함한 Map
     * @throws RuntimeException 관리자 권한이 없는 경우
     */
    @GetMapping("/statistics/summary")
    public Map<String, Object> getSummary(@AuthenticationPrincipal UserDTO principal) {

        if (principal == null) {
            throw new RuntimeException("인증이 필요합니다.");
        }

        Long userId = principal.getId();

        // 관리자 권한 체크
        if (!userService.isAdmin(userId)) {
            log.warn("관리자 권한이 없습니다. userId: " + userId);
            throw new RuntimeException("관리자 권한이 필요합니다.");
        }

        log.info("전체 통계 요약 조회 요청 - userId: " + userId);
        return accountingService.getSummary();
    }

    /**
     * 예산 초과 인원 목록을 조회합니다 (관리자 전용).
     * 
     * <p>
     * 월별 예산을 초과한 사용자 목록과 초과 금액을 반환합니다.
     * 
     * @param principal 인증된 사용자 정보
     * @return 예산 초과 인원 정보 목록
     * @throws RuntimeException 관리자 권한이 없는 경우
     */
    @GetMapping("/statistics/over-budget")
    public List<Map<String, Object>> getOverBudgetList(@AuthenticationPrincipal UserDTO principal) {

        if (principal == null) {
            throw new RuntimeException("인증이 필요합니다.");
        }

        Long userId = principal.getId();

        // 관리자 권한 체크
        if (!userService.isAdmin(userId)) {
            log.warn("관리자 권한이 없습니다. userId: " + userId);
            throw new RuntimeException("관리자 권한이 필요합니다.");
        }

        log.info("예산 초과 인원 리스트 조회 요청 - userId: " + userId);
        return accountingService.getOverBudgetList();
    }

    /**
     * 모든 통계 정보를 한번에 조회합니다 (관리자 전용, 최적화용).
     * 
     * <p>
     * 전체 통계 요약, 부서별 통계, 카테고리별 통계, 예산 초과 인원 목록을 한번에 반환합니다.
     * 
     * @param principal 인증된 사용자 정보
     * @return 모든 통계 정보를 포함한 Map
     * @throws RuntimeException 관리자 권한이 없는 경우
     */
    @GetMapping("/statistics/all")
    public Map<String, Object> getAllStatistics(@AuthenticationPrincipal UserDTO principal) {

        if (principal == null) {
            throw new RuntimeException("인증이 필요합니다.");
        }

        Long userId = principal.getId();

        // 관리자 권한 체크
        if (!userService.isAdmin(userId)) {
            log.warn("관리자 권한이 없습니다. userId: " + userId);
            throw new RuntimeException("관리자 권한이 필요합니다.");
        }

        log.info("모든 통계 정보 통합 조회 요청 - userId: " + userId);
        return accountingService.getAllStatistics();
    }

    // 한해찬 추가
    // 월별 지출 조회
    @GetMapping("/statistics/monthly-trend")
    public List<Map<String, Object>> getMonthlyExpenseTrend(
            @RequestParam(value = "status", required = false) String status,
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

        log.info("월별 지출 추이 조회 요청 - userId: " + userId + ", status: " + status);
        return accountingService.getMonthlyExpenseTrend(status);
    }
}