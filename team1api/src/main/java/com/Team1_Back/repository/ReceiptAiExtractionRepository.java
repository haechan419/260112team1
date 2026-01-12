package com.Team1_Back.repository;

import com.Team1_Back.domain.ReceiptAiExtraction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReceiptAiExtractionRepository extends JpaRepository<ReceiptAiExtraction, Long> {

    // 영수증 ID로 AI 추출 결과 조회
    Optional<ReceiptAiExtraction> findByReceiptId(Long receiptId);
}

