package com.Team1_Back.service;

// ✅ 추가: 영수증 OCR 통합 - ReceiptAiService import
import com.Team1_Back.ai.service.ReceiptAiService;
import com.Team1_Back.domain.*;
import com.Team1_Back.dto.ReceiptDTO;
import com.Team1_Back.dto.ReceiptExtractionDTO;
import com.Team1_Back.repository.*;
import com.Team1_Back.util.CustomFileUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
// ✅ 제거: ModelMapper import (수동 매핑으로 변경)
// import org.modelmapper.ModelMapper;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ReceiptServiceImpl implements ReceiptService {

    private final ReceiptUploadRepository receiptUploadRepository;
    private final ReceiptAiExtractionRepository receiptAiExtractionRepository;
    private final ReceiptVerificationRepository receiptVerificationRepository;
    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;
    private final CustomFileUtil customFileUtil;
    // ✅ 제거: ModelMapper 필드 (수동 매핑으로 변경)
    // private final ModelMapper modelMapper;
    // ✅ 추가: 영수증 OCR 통합 - ReceiptAiService 필드 주입
    private final ReceiptAiService receiptAiService;

    @Override
    public ReceiptDTO upload(Long expenseId, Long userId, MultipartFile file) {
        Expense expense = expenseRepository.findByIdAndWriterId(expenseId, userId)
                .orElseThrow();

        if (!expense.isDraft()) {
            throw new IllegalStateException("DRAFT 상태의 지출 내역에만 영수증을 업로드할 수 있습니다.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow();

        // 파일 유효성 검증
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("파일이 비어 있습니다.");
        }
        if (file.getContentType() == null || !file.getContentType().startsWith("image/")) {
            throw new RuntimeException("이미지 파일만 업로드할 수 있습니다.");
        }
        if (file.getSize() > 10 * 1024 * 1024) { // 10MB
            throw new RuntimeException("파일 크기가 너무 큽니다. 10MB 이하만 업로드 가능합니다.");
        }

        // 파일 저장
        String fileUrl = customFileUtil.saveFile(file, "receipts");

        // 파일 해시 생성 (중복 확인용)
        String fileHash = customFileUtil.generateFileHash(file);

        // ✅ 테스트 중: 중복 업로드 방지 로직 임시 비활성화
        // 중복 업로드 방지: 동일 해시가 이미 존재하면 예외
        // if (fileHash != null && receiptUploadRepository.findByFileHash(fileHash).isPresent()) {
        //     throw new RuntimeException("이미 업로드된 영수증입니다. 같은 영수증을 중복 업로드할 수 없습니다.");
        // }

        // 기존 영수증이 있으면 삭제
        receiptUploadRepository.findByExpenseId(expenseId).ifPresent(receiptUploadRepository::delete);

        // ReceiptUpload 저장
        ReceiptUpload receiptUpload = ReceiptUpload.builder()
                .expense(expense)
                .uploadedBy(user)
                .fileUrl(fileUrl)
                .fileHash(fileHash)
                .mimeType(file.getContentType())
                .build();

        ReceiptUpload saved = receiptUploadRepository.save(receiptUpload);

        // ✅ 수정: 영수증 OCR 통합 - TODO 주석을 OCR 호출 로직으로 교체
        // AI 추출 작업 실행 (Python AI 서비스 호출)
        try {
            log.info("[ReceiptService] 영수증 AI 추출 시작: receiptId={}, filename={}",
                    saved.getId(), file.getOriginalFilename());
            ReceiptExtractionDTO extractionDTO = receiptAiService.extractReceipt(file);

            log.info("[ReceiptService] AI 추출 결과 받음: merchant={}, amount={}, date={}, category={}",
                    extractionDTO.getExtractedMerchant(),
                    extractionDTO.getExtractedAmount(),
                    extractionDTO.getExtractedDate(),
                    extractionDTO.getExtractedCategory());

            // ReceiptAiExtraction 엔티티 생성 및 저장
            ReceiptAiExtraction aiExtraction = ReceiptAiExtraction.builder()
                    .receipt(saved)
                    .modelName(extractionDTO.getModelName())
                    .extractedJson(extractionDTO.getExtractedJson())
                    .extractedDate(extractionDTO.getExtractedDate())
                    .extractedAmount(extractionDTO.getExtractedAmount())
                    .extractedMerchant(extractionDTO.getExtractedMerchant())
                    .extractedCategory(extractionDTO.getExtractedCategory())
                    .extractedDescription(extractionDTO.getExtractedDescription())
                    .confidence(extractionDTO.getConfidence())
                    .build();

            receiptAiExtractionRepository.save(aiExtraction);
            log.info("[ReceiptService] 영수증 AI 추출 완료 및 DB 저장: receiptId={}, extractionId={}",
                    saved.getId(), aiExtraction.getId());

        } catch (Exception e) {
            log.error("[ReceiptService] 영수증 AI 추출 실패: receiptId={}, error={}, stackTrace={}",
                    saved.getId(), e.getMessage(), e);
            // AI 추출 실패해도 영수증 업로드는 성공으로 처리 (사용자가 나중에 수동으로 재시도 가능)
        }

        return entityToDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ReceiptDTO get(Long id, Long userId) {
        ReceiptUpload receiptUpload = receiptUploadRepository.findById(id)
                .orElseThrow();

        // 권한 확인: 본인의 지출 내역인지 확인
        if (!receiptUpload.getExpense().getWriter().getId().equals(userId)) {
            throw new RuntimeException("권한이 없습니다.");
        }

        return entityToDTO(receiptUpload);
    }

    @Override
    @Transactional(readOnly = true)
    public Resource getImage(Long id, Long userId) {
        ReceiptUpload receiptUpload = receiptUploadRepository.findById(id)
                .orElseThrow();

        // 권한 확인
        if (!receiptUpload.getExpense().getWriter().getId().equals(userId)) {
            throw new RuntimeException("권한이 없습니다.");
        }

        Path filePath = Paths.get(receiptUpload.getFileUrl());
        return customFileUtil.getFileAsResource(filePath);
    }

    @Override
    @Transactional(readOnly = true)
    public ReceiptExtractionDTO getExtraction(Long id, Long userId) {
        ReceiptUpload receiptUpload = receiptUploadRepository.findById(id)
                .orElseThrow();

        // 권한 확인
        if (!receiptUpload.getExpense().getWriter().getId().equals(userId)) {
            throw new RuntimeException("권한이 없습니다.");
        }

        ReceiptAiExtraction extraction = receiptAiExtractionRepository.findByReceiptId(id)
                .orElseThrow();

        return ReceiptExtractionDTO.builder()
                .receiptId(id)
                .modelName(extraction.getModelName())
                .extractedDate(extraction.getExtractedDate())
                .extractedAmount(extraction.getExtractedAmount())
                .extractedMerchant(extraction.getExtractedMerchant())
                .extractedCategory(extraction.getExtractedCategory())
                .extractedDescription(extraction.getExtractedDescription())
                .confidence(extraction.getConfidence())
                .extractedJson(extraction.getExtractedJson())
                .createdAt(extraction.getCreatedAt())
                .build();
    }

    @Override
    public void remove(Long id, Long userId) {
        ReceiptUpload receiptUpload = receiptUploadRepository.findById(id)
                .orElseThrow();

        // 권한 확인
        if (!receiptUpload.getExpense().getWriter().getId().equals(userId)) {
            throw new RuntimeException("권한이 없습니다.");
        }

        // DRAFT 상태인지 확인
        if (!receiptUpload.getExpense().isDraft()) {
            throw new IllegalStateException("DRAFT 상태의 지출 내역에만 영수증을 삭제할 수 있습니다.");
        }

        // 파일 삭제
        Path filePath = Paths.get(receiptUpload.getFileUrl());
        customFileUtil.deleteFile(filePath);

        // ReceiptUpload 삭제 (CASCADE로 ReceiptAiExtraction도 함께 삭제됨)
        receiptUploadRepository.delete(receiptUpload);
    }

    /**
     * ReceiptUpload 엔티티를 ReceiptDTO로 변환합니다 (수동 매핑 방식).
     *
     * <p>ModelMapper 대신 수동 매핑을 사용하여 연관 엔티티와 Repository 조회가 필요한 부분을 처리합니다.
     *
     * @param entity 변환할 ReceiptUpload 엔티티
     * @return ReceiptDTO
     */
    private ReceiptDTO entityToDTO(ReceiptUpload entity) {
        // ✅ 변경: ModelMapper 제거하고 수동 매핑으로 변경
        ReceiptDTO dto = ReceiptDTO.builder()
                .id(entity.getId())
                .fileUrl(entity.getFileUrl())
                .fileHash(entity.getFileHash())
                .mimeType(entity.getMimeType())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();

        // 2. 연관 엔티티 매핑 (수동 처리)
        if (entity.getExpense() != null) {
            dto.setExpenseId(entity.getExpense().getId());
            // Expense의 상태도 추가
            if (entity.getExpense().getStatus() != null) {
                dto.setStatus(entity.getExpense().getStatus().name());
            }
        }

        if (entity.getUploadedBy() != null) {
            dto.setUploadedBy(entity.getUploadedBy().getId());
            dto.setUploadedByName(entity.getUploadedBy().getName());
        }

        // 3. Repository 조회가 필요한 데이터 (수동 처리)
        // AI 추출 결과 추가
        Optional<ReceiptAiExtraction> extractionOpt = receiptAiExtractionRepository.findByReceiptId(entity.getId());
        if (extractionOpt.isPresent()) {
            ReceiptAiExtraction extraction = extractionOpt.get();
            dto.setExtractionId(extraction.getId());
            dto.setModelName(extraction.getModelName());
            dto.setExtractedJson(extraction.getExtractedJson());
            dto.setExtractedDate(extraction.getExtractedDate());
            dto.setExtractedAmount(extraction.getExtractedAmount());
            dto.setExtractedMerchant(extraction.getExtractedMerchant());
            dto.setExtractedCategory(extraction.getExtractedCategory());
            dto.setExtractedDescription(extraction.getExtractedDescription());
            dto.setConfidence(extraction.getConfidence());
            dto.setExtractionCreatedAt(extraction.getCreatedAt());
        }

        // 검증 결과 추가
        if (entity.getExpense() != null) {
            Optional<ReceiptVerification> verificationOpt = receiptVerificationRepository.findByExpenseId(entity.getExpense().getId());
            if (verificationOpt.isPresent()) {
                ReceiptVerification verification = verificationOpt.get();
                dto.setVerificationId(verification.getId());
                if (verification.getVerifiedBy() != null) {
                    dto.setVerifiedBy(verification.getVerifiedBy().getId());
                    dto.setVerifiedByName(verification.getVerifiedBy().getName());
                }
                dto.setVerifiedMerchant(verification.getVerifiedMerchant());
                dto.setVerifiedAmount(verification.getVerifiedAmount());
                dto.setVerifiedCategory(verification.getVerifiedCategory());
                dto.setReason(verification.getReason());
                dto.setVerificationCreatedAt(verification.getCreatedAt());
            }
        }

        return dto;
    }
}

