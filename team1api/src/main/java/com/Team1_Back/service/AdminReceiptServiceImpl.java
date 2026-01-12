package com.Team1_Back.service;

import com.Team1_Back.domain.*;
import com.Team1_Back.dto.PageRequestDTO;
import com.Team1_Back.dto.PageResponseDTO;
import com.Team1_Back.dto.ReceiptDTO;
import com.Team1_Back.dto.ReceiptVerificationDTO;
import com.Team1_Back.repository.*;
import com.Team1_Back.util.CustomFileUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class AdminReceiptServiceImpl implements AdminReceiptService {

    private final ReceiptUploadRepository receiptUploadRepository;
    private final ReceiptAiExtractionRepository receiptAiExtractionRepository;
    private final ExpenseRepository expenseRepository;
    private final ReceiptVerificationRepository receiptVerificationRepository;
    private final ApprovalRequestRepository approvalRequestRepository;
    private final ApprovalActionLogRepository approvalActionLogRepository;
    private final UserRepository userRepository;
    private final CustomFileUtil customFileUtil;
    private final ModelMapper modelMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<ReceiptDTO> getList(PageRequestDTO pageRequestDTO, String status, Long approverId) {
        if (pageRequestDTO == null) {
            pageRequestDTO = PageRequestDTO.builder().page(1).size(15).build();
        }

        // 상신일 기준 정렬 (최근 상신일 먼저), 같은 날짜면 최근 업데이트 먼저
        Pageable pageable = pageRequestDTO.getPageable("createdAt", "updatedAt");

        log.info("getList..............");

        // Expense 테이블에서 제출된 지출 내역 조회 (영수증 유무와 관계없이)
        // DRAFT 상태는 제외 (아직 제출 안 된 것)
        Page<Expense> expensePage;
        if (status != null && !status.isEmpty()) {
            try {
                com.Team1_Back.domain.ApprovalStatus statusEnum = com.Team1_Back.domain.ApprovalStatus.valueOf(status);
                expensePage = expenseRepository.findByStatus(statusEnum, pageable);
            } catch (IllegalArgumentException e) {
                // 잘못된 상태값이면 DRAFT 제외한 전체 조회
                expensePage = expenseRepository.findAllSubmitted(pageable);
            }
        } else {
            // 상태 필터가 없으면 DRAFT를 제외한 모든 제출된 지출 내역 조회
            expensePage = expenseRepository.findAllSubmitted(pageable);
        }

        List<Expense> expenses = expensePage.getContent();

        // 각 지출 내역을 ReceiptDTO로 변환 (영수증이 있으면 영수증 정보 포함, 없으면 null)
        List<ReceiptDTO> dtoList = expenses.stream()
                .map(this::expenseToDTO)
                .filter(dto -> dto != null)
                .collect(Collectors.toList());

        return PageResponseDTO.of(
                dtoList,
                pageRequestDTO,
                expensePage.getTotalElements()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ReceiptDTO get(Long id) {
        com.Team1_Back.domain.ReceiptUpload receiptUpload = receiptUploadRepository.findById(id)
                .orElseThrow();

        return entityToDTO(receiptUpload);
    }

    @Override
    @Transactional(readOnly = true)
    public Resource getImage(Long id) {
        com.Team1_Back.domain.ReceiptUpload receiptUpload = receiptUploadRepository.findById(id)
                .orElseThrow(() -> new java.util.NoSuchElementException("영수증을 찾을 수 없습니다."));

        // file_url이 상대 경로인 경우 절대 경로로 변환
        String fileUrl = receiptUpload.getFileUrl();
        Path filePath = Paths.get(fileUrl);

        // CustomFileUtil.getFileAsResource()가 NoSuchElementException을 던지면 그대로 전파 (ControllerAdvice가 404로 처리)
        return customFileUtil.getFileAsResource(filePath);
    }

    @Override
    @Transactional(readOnly = true)
    public com.Team1_Back.dto.ReceiptExtractionDTO getExtraction(Long id) {
        // 영수증 존재 여부 확인
        if (!receiptUploadRepository.existsById(id)) {
            throw new java.util.NoSuchElementException("영수증을 찾을 수 없습니다.");
        }

        // OCR 추출 결과 조회 (없을 경우 예외 발생)
        ReceiptAiExtraction extraction = receiptAiExtractionRepository.findByReceiptId(id)
                .orElseThrow(() -> new java.util.NoSuchElementException("OCR 추출 결과를 찾을 수 없습니다. 영수증 업로드 후 OCR 처리가 완료되지 않았거나 실패한 것 같습니다."));

        return com.Team1_Back.dto.ReceiptExtractionDTO.builder()
                .receiptId(id)
                .modelName(extraction.getModelName())
                .extractedDate(extraction.getExtractedDate())
                .extractedAmount(extraction.getExtractedAmount())
                .extractedMerchant(extraction.getExtractedMerchant())
                .extractedCategory(extraction.getExtractedCategory())
                .confidence(extraction.getConfidence())
                .extractedJson(extraction.getExtractedJson())
                .createdAt(extraction.getCreatedAt())
                .build();
    }

    @Override
    public void verify(Long id, ReceiptVerificationDTO verificationDTO, Long adminId) {
        Expense expense;

        // 영수증이 있는 경우와 없는 경우 모두 처리
        if (id != null) {
            // 영수증이 있는 경우
            com.Team1_Back.domain.ReceiptUpload receiptUpload = receiptUploadRepository.findById(id)
                    .orElseThrow();
            expense = receiptUpload.getExpense();
        } else {
            // 영수증이 없는 경우: expenseId로 직접 조회
            Long expenseId = verificationDTO.getExpenseId();
            if (expenseId == null) {
                throw new RuntimeException("expenseId가 필요합니다.");
            }
            expense = expenseRepository.findById(expenseId)
                    .orElseThrow();
        }
        User admin = userRepository.findById(adminId)
                .orElseThrow();

        if (!admin.isAdmin()) {
            throw new RuntimeException("관리자 권한이 필요합니다.");
        }

        String action = verificationDTO.getAction();

        // Expense 상태 업데이트
        if ("APPROVE".equals(action)) {
            expense.approve();
            if (verificationDTO.getVerifiedMerchant() != null) {
                expense.setMerchant(verificationDTO.getVerifiedMerchant());
            }
            if (verificationDTO.getVerifiedAmount() != null) {
                expense.setAmount(verificationDTO.getVerifiedAmount());
            }
            if (verificationDTO.getVerifiedCategory() != null) {
                expense.setCategory(verificationDTO.getVerifiedCategory());
            }
        } else if ("REJECT".equals(action)) {
            expense.reject(verificationDTO.getReason());
        } else if ("REQUEST_MORE_INFO".equals(action)) {
            expense.requestMoreInfo(verificationDTO.getReason());
        }

        expenseRepository.save(expense);

        // ApprovalRequest 상태 동기화
        ApprovalRequest approvalRequest = approvalRequestRepository
                .findByRequestTypeAndRefId("EXPENSE", expense.getId())
                .orElse(null);

        if (approvalRequest != null) {
            approvalRequest.syncStatusSnapshot(expense.getStatus());
            approvalRequestRepository.save(approvalRequest);

            // ApprovalActionLog 생성
            ApprovalActionLog actionLog = ApprovalActionLog.builder()
                    .approvalRequest(approvalRequest)
                    .actor(admin)
                    .action(action)
                    .message(verificationDTO.getReason())
                    .build();

            approvalActionLogRepository.save(actionLog);
        }

        // ReceiptVerification 저장 (기존 검증이 있으면 삭제 후 새로 생성)
        receiptVerificationRepository.findByExpenseId(expense.getId())
                .ifPresent(receiptVerificationRepository::delete);

        ReceiptVerification verification = ReceiptVerification.builder()
                .expense(expense)
                .verifiedBy(admin)
                .verifiedMerchant(verificationDTO.getVerifiedMerchant())
                .verifiedAmount(verificationDTO.getVerifiedAmount())
                .verifiedCategory(verificationDTO.getVerifiedCategory())
                .reason(verificationDTO.getReason())
                .build();

        receiptVerificationRepository.save(verification);
    }

    /**
     * ReceiptUpload 엔티티를 ReceiptDTO로 변환합니다 (하이브리드 방식).
     *
     * <p>ModelMapper로 기본 필드를 매핑하고, 연관 엔티티와 Repository 조회가 필요한 부분은 수동으로 처리합니다.
     *
     * @param entity 변환할 ReceiptUpload 엔티티
     * @return ReceiptDTO (필수 연관 엔티티가 null이면 null 반환)
     */
    private ReceiptDTO entityToDTO(com.Team1_Back.domain.ReceiptUpload entity) {
        Expense expense = entity.getExpense();
        User uploadedBy = entity.getUploadedBy();

        if (expense == null || uploadedBy == null) {
            return null;
        }

        // 1. ModelMapper로 기본 필드 매핑
        ReceiptDTO dto = modelMapper.map(entity, ReceiptDTO.class);

        // 2. 연관 엔티티 매핑 (수동 처리)
        dto.setExpenseId(expense.getId());
        dto.setUploadedBy(uploadedBy.getId());
        dto.setUploadedByName(uploadedBy.getName());

        // Expense의 상태도 추가
        if (expense.getStatus() != null) {
            dto.setStatus(expense.getStatus().name());
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
        receiptVerificationRepository.findByExpenseId(entity.getExpense().getId()).ifPresent(verification -> {
            User verifiedBy = verification.getVerifiedBy();
            if (verifiedBy != null) {
                dto.setVerificationId(verification.getId());
                dto.setVerifiedBy(verifiedBy.getId());
                dto.setVerifiedByName(verifiedBy.getName());
                dto.setVerifiedMerchant(verification.getVerifiedMerchant());
                dto.setVerifiedAmount(verification.getVerifiedAmount());
                dto.setVerifiedCategory(verification.getVerifiedCategory());
                dto.setReason(verification.getReason());
                dto.setVerificationCreatedAt(verification.getCreatedAt());
            } else {
                log.warn("⚠️ ReceiptVerification ID {}의 verifiedBy가 null입니다.", verification.getId());
            }
        });

        return dto;
    }

    /**
     * Expense 엔티티를 ReceiptDTO로 변환합니다 (하이브리드 방식, 영수증이 없어도 처리 가능).
     *
     * <p>ModelMapper로 기본 필드를 매핑하고, 연관 엔티티, Enum 변환, Repository 조회가 필요한 부분은 수동으로 처리합니다.
     * 영수증이 없는 경우에도 Expense 정보만으로 ReceiptDTO를 생성할 수 있습니다.
     *
     * @param expense 변환할 Expense 엔티티
     * @return ReceiptDTO (필수 필드가 null이면 null 반환)
     */
    private ReceiptDTO expenseToDTO(Expense expense) {
        User writer = expense.getWriter();

        if (writer == null || expense.getStatus() == null) {
            return null;
        }

        // 1. ModelMapper로 기본 필드 매핑 (Expense → ReceiptDTO는 직접 매핑 불가하므로 수동 생성)
        // Expense와 ReceiptDTO는 구조가 다르므로 기본 DTO 생성
        ReceiptDTO dto = ReceiptDTO.builder()
                .expenseId(expense.getId())
                .createdAt(expense.getCreatedAt())
                .updatedAt(expense.getUpdatedAt())
                .build();

        // 2. 연관 엔티티 매핑 (수동 처리)
        dto.setUploadedBy(writer.getId());
        dto.setUploadedByName(writer.getName());

        // 3. Enum → String 변환 (수동 처리)
        dto.setStatus(expense.getStatus().name());

        // 4. Repository 조회가 필요한 데이터 (수동 처리)
        // 영수증이 있는지 확인
        Optional<com.Team1_Back.domain.ReceiptUpload> receiptOpt = receiptUploadRepository.findByExpenseId(expense.getId());

        // 영수증이 있으면 영수증 정보 포함
        if (receiptOpt.isPresent()) {
            com.Team1_Back.domain.ReceiptUpload receipt = receiptOpt.get();
            // ModelMapper로 ReceiptUpload → ReceiptDTO 기본 매핑
            ReceiptDTO receiptDto = modelMapper.map(receipt, ReceiptDTO.class);
            // 기본 정보는 이미 설정했으므로 영수증 정보만 추가
            dto.setId(receiptDto.getId());
            dto.setFileUrl(receiptDto.getFileUrl());
            dto.setFileHash(receiptDto.getFileHash());
            dto.setMimeType(receiptDto.getMimeType());

            // AI 추출 결과 추가
            Optional<ReceiptAiExtraction> extractionOpt = receiptAiExtractionRepository.findByReceiptId(receipt.getId());
            if (extractionOpt.isPresent()) {
                ReceiptAiExtraction extraction = extractionOpt.get();
                dto.setExtractionId(extraction.getId());
                dto.setModelName(extraction.getModelName());
                dto.setExtractedJson(extraction.getExtractedJson());
                dto.setExtractedDate(extraction.getExtractedDate());
                dto.setExtractedAmount(extraction.getExtractedAmount());
                dto.setExtractedMerchant(extraction.getExtractedMerchant());
                dto.setExtractedCategory(extraction.getExtractedCategory());
                dto.setConfidence(extraction.getConfidence());
                dto.setExtractionCreatedAt(extraction.getCreatedAt());
            }
        } else {
            // 영수증이 없으면 id는 null, fileUrl 등도 null
            dto.setId(null);
            dto.setFileUrl(null);
            dto.setFileHash(null);
            dto.setMimeType(null);
        }

        // 검증 결과 추가 (영수증 유무와 관계없이)
        receiptVerificationRepository.findByExpenseId(expense.getId()).ifPresent(verification -> {
            User verifiedBy = verification.getVerifiedBy();
            if (verifiedBy != null) {
                dto.setVerificationId(verification.getId());
                dto.setVerifiedBy(verifiedBy.getId());
                dto.setVerifiedByName(verifiedBy.getName());
                dto.setVerifiedMerchant(verification.getVerifiedMerchant());
                dto.setVerifiedAmount(verification.getVerifiedAmount());
                dto.setVerifiedCategory(verification.getVerifiedCategory());
                dto.setReason(verification.getReason());
                dto.setVerificationCreatedAt(verification.getCreatedAt());
            }
        });

        return dto;
    }
}

