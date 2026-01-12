package com.Team1_Back.service;

import com.Team1_Back.dto.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 승인 요청 비즈니스 로직을 처리하는 서비스 인터페이스
 * 
 * @author Team1
 */
public interface ApprovalService {

    /**
     * 승인 요청 목록을 조회합니다.
     * 
     * @param userId 요청한 사용자 ID
     * @param isAdmin 관리자 여부
     * @param pageRequestDTO 페이지네이션 정보
     * @param requestType 요청 유형 (EXPENSE, STORE_ORDER 등, 선택)
     * @param status 승인 상태 (선택)
     * @param startDate 조회 시작 날짜 (선택)
     * @param endDate 조회 종료 날짜 (선택)
     * @return 페이지네이션된 승인 요청 목록
     */
    PageResponseDTO<ApprovalRequestDTO> getList(Long userId, boolean isAdmin, PageRequestDTO pageRequestDTO, String requestType, String status, LocalDate startDate, LocalDate endDate);

    /**
     * 특정 승인 요청을 조회합니다.
     * 
     * @param id 승인 요청 ID
     * @param userId 요청한 사용자 ID
     * @param isAdmin 관리자 여부
     * @return 승인 요청 정보
     */
    ApprovalRequestDTO get(Long id, Long userId, boolean isAdmin);

    /**
     * 승인 요청의 처리 이력을 조회합니다.
     * 
     * @param id 승인 요청 ID
     * @param userId 요청한 사용자 ID
     * @param isAdmin 관리자 여부
     * @return 승인 처리 이력 목록
     */
    List<ApprovalActionLogDTO> getLogs(Long id, Long userId, boolean isAdmin);

    /**
     * 승인 요청에 대한 처리 액션을 수행합니다.
     * 
     * @param id 승인 요청 ID
     * @param actionDTO 처리 액션 정보
     * @param adminId 처리하는 관리자 ID
     * @return 처리된 승인 요청 정보
     */
    ApprovalRequestDTO action(Long id, ApprovalActionDTO actionDTO, Long adminId);
}

