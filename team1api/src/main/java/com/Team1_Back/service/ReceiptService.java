package com.Team1_Back.service;

import com.Team1_Back.dto.ReceiptDTO;
import com.Team1_Back.dto.ReceiptExtractionDTO;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/**
 * 영수증 비즈니스 로직을 처리하는 서비스 인터페이스
 * 
 * @author Team1
 */
public interface ReceiptService {

    /**
     * 영수증 이미지를 업로드합니다.
     * 
     * @param expenseId 영수증을 첨부할 지출 내역 ID
     * @param userId 업로드하는 사용자 ID
     * @param file 업로드할 영수증 이미지 파일
     * @return 업로드된 영수증 정보
     */
    ReceiptDTO upload(Long expenseId, Long userId, MultipartFile file);

    /**
     * 특정 영수증을 조회합니다.
     * 
     * @param id 영수증 ID
     * @param userId 요청한 사용자 ID
     * @return 영수증 정보
     */
    ReceiptDTO get(Long id, Long userId);

    /**
     * 영수증 이미지를 조회합니다.
     * 
     * @param id 영수증 ID
     * @param userId 요청한 사용자 ID
     * @return 영수증 이미지 리소스
     */
    Resource getImage(Long id, Long userId);

    /**
     * 영수증에서 추출된 정보를 조회합니다.
     * 
     * @param id 영수증 ID
     * @param userId 요청한 사용자 ID
     * @return 영수증 추출 정보
     */
    ReceiptExtractionDTO getExtraction(Long id, Long userId);

    /**
     * 영수증을 삭제합니다.
     * 
     * @param id 영수증 ID
     * @param userId 요청한 사용자 ID
     */
    void remove(Long id, Long userId);
}

