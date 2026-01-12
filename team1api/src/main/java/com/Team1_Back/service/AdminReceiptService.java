package com.Team1_Back.service;

import com.Team1_Back.dto.PageRequestDTO;
import com.Team1_Back.dto.PageResponseDTO;
import com.Team1_Back.dto.ReceiptDTO;
import com.Team1_Back.dto.ReceiptVerificationDTO;
import org.springframework.core.io.Resource;

/**
 * 관리자용 영수증 비즈니스 로직을 처리하는 서비스 인터페이스
 * 
 * @author Team1
 */
public interface AdminReceiptService {

    /**
     * 영수증 목록을 조회합니다 (관리자 전용).
     * 
     * @param pageRequestDTO 페이지네이션 정보
     * @param status 필터링할 승인 상태 (선택)
     * @param approverId 승인자 ID로 필터링 (선택)
     * @return 페이지네이션된 영수증 목록
     */
    PageResponseDTO<ReceiptDTO> getList(PageRequestDTO pageRequestDTO, String status, Long approverId);

    /**
     * 특정 영수증을 조회합니다 (관리자 전용).
     * 
     * @param id 영수증 ID
     * @return 영수증 정보
     */
    ReceiptDTO get(Long id);

    /**
     * 영수증 이미지를 조회합니다 (관리자 전용).
     * 
     * @param id 영수증 ID
     * @return 영수증 이미지 리소스
     */
    Resource getImage(Long id);

    /**
     * 영수증에서 추출된 정보를 조회합니다 (관리자 전용).
     * 
     * @param id 영수증 ID
     * @return 영수증 추출 정보
     */
    com.Team1_Back.dto.ReceiptExtractionDTO getExtraction(Long id);

    /**
     * 영수증을 검증합니다 (관리자 전용).
     * 
     * @param id 영수증 ID (null일 수 있음, expenseId로 검증 가능)
     * @param verificationDTO 검증 정보
     * @param adminId 검증하는 관리자 ID
     */
    void verify(Long id, ReceiptVerificationDTO verificationDTO, Long adminId);
}

