package com.Team1_Back.repository;

import com.Team1_Back.domain.Expense;
import com.Team1_Back.domain.ReceiptAiExtraction;
import com.Team1_Back.domain.ReceiptUpload;
import com.Team1_Back.domain.ReceiptVerification;
import com.Team1_Back.domain.User;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@SpringBootTest
@Slf4j
public class ReceiptUploadRepositoryTests {

    @Autowired
    private ReceiptUploadRepository receiptUploadRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private ReceiptAiExtractionRepository receiptAiExtractionRepository;

    @Autowired
    private ReceiptVerificationRepository receiptVerificationRepository;

    @Autowired
    private UserRepository userRepository;

    // ========== 더미 데이터 생성 메서드 ==========

    /**
     * 영수증 업로드 더미 데이터 생성
     * ExpenseRepositoryTests.insertDummyExpenses() 실행 후 사용
     */
    @Test
    public void insertDummyReceiptUploads() {
        // 제출된 지출 내역 중 일부에 영수증 업로드 데이터 생성
        List<Expense> submittedExpenses = expenseRepository.findAll().stream()
                .filter(e -> e.getStatus() != com.Team1_Back.domain.ApprovalStatus.DRAFT)
                .limit(12)
                .toList();

        for (Expense expense : submittedExpenses) {
            // 이미 영수증이 있는지 확인
            if (receiptUploadRepository.findByExpenseId(expense.getId()).isEmpty()) {
                ReceiptUpload receiptUpload = ReceiptUpload.builder()
                        .expense(expense)
                        .uploadedBy(expense.getWriter())
                        .fileUrl("/uploads/receipts/receipt_" + expense.getId() + ".jpg")
                        .fileHash("hash_" + expense.getId() + "_" + expense.getWriter().getId())
                        .mimeType("image/jpeg")
                        .build();
                receiptUploadRepository.save(receiptUpload);
                log.info("영수증 업로드 생성: Expense ID={}", expense.getId());
            }
        }

        log.info("영수증 업로드 더미 데이터 생성 완료");
    }

    /**
     * AI 추출 결과 더미 데이터 생성
     * ReceiptUploadRepositoryTests.insertDummyReceiptUploads() 실행 후 사용
     */
    @Test
    @Transactional
    public void insertDummyReceiptAiExtractions() {
        List<ReceiptUpload> receiptUploads = receiptUploadRepository.findAll().stream()
                .limit(8)
                .toList();

        for (ReceiptUpload receiptUpload : receiptUploads) {
            // 이미 AI 추출 결과가 있는지 확인
            if (receiptAiExtractionRepository.findByReceiptId(receiptUpload.getId()).isEmpty()) {
                Expense expense = receiptUpload.getExpense();
                
                // JSON 문자열 생성
                String extractedJson = String.format(
                    "{\"date\":\"%s\",\"amount\":%d,\"merchant\":\"%s\",\"category\":\"%s\"}",
                    expense.getReceiptDate(),
                    expense.getAmount(),
                    expense.getMerchant(),
                    expense.getCategory()
                );

                ReceiptAiExtraction extraction = ReceiptAiExtraction.builder()
                        .receipt(receiptUpload)
                        .modelName("gpt-4-vision")
                        .extractedJson(extractedJson)
                        .extractedDate(expense.getReceiptDate())
                        .extractedAmount(expense.getAmount())
                        .extractedMerchant(expense.getMerchant())
                        .extractedCategory(expense.getCategory())
                        .confidence(java.math.BigDecimal.valueOf(0.90))
                        .build();
                receiptAiExtractionRepository.save(extraction);
                log.info("AI 추출 결과 생성: Receipt ID={}", receiptUpload.getId());
            }
        }

        log.info("AI 추출 결과 더미 데이터 생성 완료");
    }

    /**
     * 영수증 검증 더미 데이터 생성
     * ReceiptUploadRepositoryTests.insertDummyReceiptUploads() 실행 후 사용
     */
    @Test
    public void insertDummyReceiptVerifications() {
        User admin = userRepository.findByEmployeeNo("20250002")
                .orElseThrow(() -> new RuntimeException("20250002 관리자를 찾을 수 없습니다."));

        // 승인된 지출 내역 중 영수증이 있는 것들
        List<Expense> approvedExpenses = expenseRepository.findAll().stream()
                .filter(e -> e.getStatus() == com.Team1_Back.domain.ApprovalStatus.APPROVED)
                .filter(e -> receiptUploadRepository.findByExpenseId(e.getId()).isPresent())
                .limit(5)
                .toList();

        for (Expense expense : approvedExpenses) {
            // 이미 검증 결과가 있는지 확인
            if (receiptVerificationRepository.findByExpenseId(expense.getId()).isEmpty()) {
                ReceiptVerification verification = ReceiptVerification.builder()
                        .expense(expense)
                        .verifiedBy(admin)
                        .verifiedMerchant(expense.getMerchant())
                        .verifiedAmount(expense.getAmount())
                        .verifiedCategory(expense.getCategory())
                        .reason("AI 추출 결과와 일치합니다.")
                        .build();
                receiptVerificationRepository.save(verification);
                log.info("영수증 검증 생성: Expense ID={}", expense.getId());
            }
        }

        log.info("영수증 검증 더미 데이터 생성 완료");
    }
}

