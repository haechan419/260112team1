package com.Team1_Back.ai.service;

import com.Team1_Back.dto.ReceiptExtractionDTO;
import org.springframework.web.multipart.MultipartFile;

/**
 * ✅ 새로 생성: 영수증 OCR 통합 - 영수증 AI 추출 서비스 인터페이스
 * Python AI 서비스를 호출하여 영수증 이미지에서 정보를 추출합니다.
 * 
 * @author Team1
 */
public interface ReceiptAiService {

    /**
     * 영수증 이미지를 분석하여 구조화된 데이터를 추출합니다.
     * 
     * @param imageFile 분석할 영수증 이미지 파일
     * @return 추출된 영수증 정보 (extractedMerchant, extractedAmount, extractedDate, extractedCategory, extractedJson 등)
     * @throws RuntimeException AI 서비스 호출 실패 시
     */
    ReceiptExtractionDTO extractReceipt(MultipartFile imageFile);
}

