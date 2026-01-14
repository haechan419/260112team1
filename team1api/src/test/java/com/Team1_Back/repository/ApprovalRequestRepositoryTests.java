package com.Team1_Back.repository;

import com.Team1_Back.domain.ApprovalActionLog;
import com.Team1_Back.domain.ApprovalRequest;
import com.Team1_Back.domain.ApprovalStatus;
import com.Team1_Back.domain.Expense;
import com.Team1_Back.domain.User;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Slf4j
public class ApprovalRequestRepositoryTests {

    @Autowired
    private ApprovalRequestRepository approvalRequestRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ApprovalActionLogRepository approvalActionLogRepository;

    @Test
    @Transactional
    public void testInsert() {
        // given
        User requester = userRepository.findByEmployeeNo("20250001")
                .orElseThrow(() -> new RuntimeException("테스트 사용자를 찾을 수 없습니다."));

        User approver = userRepository.findByEmployeeNo("20250002")
                .orElseGet(() -> requester); // 20250002가 없으면 requester 사용

        ApprovalRequest approvalRequest = ApprovalRequest.builder()
                .requestType("EXPENSE")
                .refId(1L)
                .requester(requester)
                .approver(approver)
                .statusSnapshot(ApprovalStatus.SUBMITTED)
                .build();

        // when
        ApprovalRequest saved = approvalRequestRepository.save(approvalRequest);

        // then
        assertNotNull(saved.getId());
        log.info("저장된 결재 요청 ID: {}", saved.getId());
        log.info("저장된 결재 요청: {}", saved);
    }

    @Test
    @Transactional
    public void testFindByRequesterId() {
        // given
        User requester = userRepository.findByEmployeeNo("20250001")
                .orElseThrow(() -> new RuntimeException("테스트 사용자를 찾을 수 없습니다."));

        Pageable pageable = PageRequest.of(0, 10, Sort.by("updatedAt").descending());

        // when
        Page<ApprovalRequest> result = approvalRequestRepository.findByRequesterId(
                requester.getId(),
                pageable
        );

        // then
        assertNotNull(result);
        log.info("요청자별 결재 요청 개수: {}", result.getTotalElements());
        result.getContent().forEach(request -> {
            assertEquals(requester.getId(), request.getRequester().getId());
            log.info("결재 요청: {}", request);
        });
    }

    @Test
    @Transactional
    public void testFindByRequesterIdAndStatusSnapshot() {
        // given
        User requester = userRepository.findByEmployeeNo("20250001")
                .orElseThrow(() -> new RuntimeException("테스트 사용자를 찾을 수 없습니다."));

        Pageable pageable = PageRequest.of(0, 10, Sort.by("updatedAt").descending());

        // when
        Page<ApprovalRequest> result = approvalRequestRepository.findByRequesterIdAndStatusSnapshot(
                requester.getId(),
                ApprovalStatus.APPROVED,
                pageable
        );

        // then
        assertNotNull(result);
        log.info("APPROVED 상태 결재 요청 개수: {}", result.getTotalElements());
        result.getContent().forEach(request -> {
            assertEquals(ApprovalStatus.APPROVED, request.getStatusSnapshot());
            assertEquals(requester.getId(), request.getRequester().getId());
            log.info("결재 요청: {}", request);
        });
    }

    @Test
    @Transactional
    public void testFindByRequestType() {
        // given
        Pageable pageable = PageRequest.of(0, 10, Sort.by("updatedAt").descending());

        // when
        Page<ApprovalRequest> result = approvalRequestRepository.findByRequestType(
                "EXPENSE",
                pageable
        );

        // then
        assertNotNull(result);
        log.info("EXPENSE 타입 결재 요청 개수: {}", result.getTotalElements());
        result.getContent().forEach(request -> {
            assertEquals("EXPENSE", request.getRequestType());
            log.info("결재 요청: {}", request);
        });
    }

    @Test
    @Transactional
    public void testFindByStatusSnapshot() {
        // given
        Pageable pageable = PageRequest.of(0, 10, Sort.by("updatedAt").descending());

        // when
        Page<ApprovalRequest> result = approvalRequestRepository.findByStatusSnapshot(
                ApprovalStatus.SUBMITTED,
                pageable
        );

        // then
        assertNotNull(result);
        log.info("SUBMITTED 상태 결재 요청 개수: {}", result.getTotalElements());
        result.getContent().forEach(request -> {
            assertEquals(ApprovalStatus.SUBMITTED, request.getStatusSnapshot());
            log.info("결재 요청: {}", request);
        });
    }

    @Test
    @Transactional
    public void testFindByRequestTypeAndStatusSnapshot() {
        // given
        Pageable pageable = PageRequest.of(0, 10, Sort.by("updatedAt").descending());

        // when
        Page<ApprovalRequest> result = approvalRequestRepository.findByRequestTypeAndStatusSnapshot(
                "EXPENSE",
                ApprovalStatus.APPROVED,
                pageable
        );

        // then
        assertNotNull(result);
        log.info("EXPENSE 타입 + APPROVED 상태 결재 요청 개수: {}", result.getTotalElements());
        result.getContent().forEach(request -> {
            assertEquals("EXPENSE", request.getRequestType());
            assertEquals(ApprovalStatus.APPROVED, request.getStatusSnapshot());
            log.info("결재 요청: {}", request);
        });
    }

    @Test
    @Transactional
    public void testFindByRequestTypeAndRefId() {
        // given
        User requester = userRepository.findByEmployeeNo("20250001")
                .orElseThrow(() -> new RuntimeException("테스트 사용자를 찾을 수 없습니다."));

        // 먼저 Expense 생성
        Expense expense = Expense.builder()
                .writer(requester)
                .status(ApprovalStatus.SUBMITTED)
                .merchant("결재 요청 테스트")
                .amount(20000)
                .category("식비")
                .receiptDate(java.time.LocalDate.now())
                .build();
        Expense savedExpense = expenseRepository.save(expense);

        // ApprovalRequest 생성
        ApprovalRequest approvalRequest = ApprovalRequest.builder()
                .requestType("EXPENSE")
                .refId(savedExpense.getId())
                .requester(requester)
                .statusSnapshot(ApprovalStatus.SUBMITTED)
                .build();
        approvalRequestRepository.save(approvalRequest);

        // when
        Optional<ApprovalRequest> result = approvalRequestRepository.findByRequestTypeAndRefId(
                "EXPENSE",
                savedExpense.getId()
        );

        // then
        assertTrue(result.isPresent());
        assertEquals("EXPENSE", result.get().getRequestType());
        assertEquals(savedExpense.getId(), result.get().getRefId());
        log.info("조회된 결재 요청: {}", result.get());
    }

    @Test
    @Transactional
    public void testFindByApproverId() {
        // given
        User approver = userRepository.findByEmployeeNo("20250002")
                .orElseGet(() -> {
                    // 20250002가 없으면 20250001 사용
                    return userRepository.findByEmployeeNo("20250001")
                            .orElseThrow(() -> new RuntimeException("테스트 사용자를 찾을 수 없습니다."));
                });

        Pageable pageable = PageRequest.of(0, 10, Sort.by("updatedAt").descending());

        // when
        Page<ApprovalRequest> result = approvalRequestRepository.findByApproverId(
                approver.getId(),
                pageable
        );

        // then
        assertNotNull(result);
        log.info("결재자별 결재 요청 개수: {}", result.getTotalElements());
        result.getContent().forEach(request -> {
            if (request.getApprover() != null) {
                assertEquals(approver.getId(), request.getApprover().getId());
            }
            log.info("결재 요청: {}", request);
        });
    }

    // ========== 더미 데이터 생성 메서드 ==========

    /**
     * 결재 요청 더미 데이터 생성
     * ExpenseRepositoryTests.insertDummyExpenses() 실행 후 사용
     */
    @Test
    public void insertDummyApprovalRequests() {
        User admin = userRepository.findByEmployeeNo("20250002")
                .orElseThrow(() -> new RuntimeException("20250002 관리자를 찾을 수 없습니다."));

        // 제출된 지출 내역에 대해 결재 요청 생성
        List<Expense> submittedExpenses = expenseRepository.findAll().stream()
                .filter(e -> e.getStatus() != ApprovalStatus.DRAFT)
                .toList();

        for (Expense expense : submittedExpenses) {
            // 이미 결재 요청이 있는지 확인
            Optional<ApprovalRequest> existing = approvalRequestRepository
                    .findByRequestTypeAndRefId("EXPENSE", expense.getId());

            if (existing.isEmpty()) {
                ApprovalRequest approvalRequest = ApprovalRequest.builder()
                        .requestType("EXPENSE")
                        .refId(expense.getId())
                        .requester(expense.getWriter())
                        .approver(admin)
                        .statusSnapshot(expense.getStatus())
                        .build();
                approvalRequestRepository.save(approvalRequest);
                log.info("결재 요청 생성: Expense ID={}, Status={}", expense.getId(), expense.getStatus());
            }
        }

        log.info("결재 요청 더미 데이터 생성 완료");
    }

    /**
     * 결재 액션 로그 더미 데이터 생성
     * ApprovalRequestRepositoryTests.insertDummyApprovalRequests() 실행 후 사용
     */
    @Test
    public void insertDummyApprovalActionLogs() {
        List<ApprovalRequest> approvalRequests = approvalRequestRepository.findAll();

        for (ApprovalRequest ar : approvalRequests) {
            // 제출 로그 생성 (모든 결재 요청에 대해)
            boolean hasSubmitLog = approvalActionLogRepository.findByApprovalRequestIdOrderByCreatedAtAsc(ar.getId())
                    .stream()
                    .anyMatch(log -> "SUBMIT".equals(log.getAction()));

            if (!hasSubmitLog) {
                ApprovalActionLog submitLog = ApprovalActionLog.builder()
                        .approvalRequest(ar)
                        .actor(ar.getRequester())
                        .action("SUBMIT")
                        .message("지출 내역을 제출했습니다.")
                        .build();
                approvalActionLogRepository.save(submitLog);
                log.info("제출 로그 생성: ApprovalRequest ID={}", ar.getId());
            }

            // 상태별 액션 로그 생성
            if (ar.getStatusSnapshot() == ApprovalStatus.APPROVED && ar.getApprover() != null) {
                boolean hasApproveLog = approvalActionLogRepository.findByApprovalRequestIdOrderByCreatedAtAsc(ar.getId())
                        .stream()
                        .anyMatch(log -> "APPROVE".equals(log.getAction()));

                if (!hasApproveLog) {
                    ApprovalActionLog approveLog = ApprovalActionLog.builder()
                            .approvalRequest(ar)
                            .actor(ar.getApprover())
                            .action("APPROVE")
                            .message("지출 내역을 승인했습니다.")
                            .build();
                    approvalActionLogRepository.save(approveLog);
                    log.info("승인 로그 생성: ApprovalRequest ID={}", ar.getId());
                }
            } else if (ar.getStatusSnapshot() == ApprovalStatus.REJECTED && ar.getApprover() != null) {
                boolean hasRejectLog = approvalActionLogRepository.findByApprovalRequestIdOrderByCreatedAtAsc(ar.getId())
                        .stream()
                        .anyMatch(log -> "REJECT".equals(log.getAction()));

                if (!hasRejectLog) {
                    Expense expense = expenseRepository.findById(ar.getRefId())
                            .orElse(null);
                    
                    String message = "지출 내역이 명세서 기준에 부합하지 않아 반려합니다. 재제출 시 보완 부탁드립니다.";
                    if (expense != null) {
                        if (expense.getMerchant() != null && expense.getMerchant().contains("맥도날드")) {
                            message = "개인 용도의 지출로 판단되어 반려합니다. 업무 관련 지출만 승인 가능합니다.";
                        } else if (expense.getMerchant() != null && expense.getMerchant().contains("이마트") && expense.getAmount() > 40000) {
                            message = "금액이 과도하여 추가 확인이 필요합니다. 구매 내역서를 첨부해주세요.";
                        }
                    }

                    ApprovalActionLog rejectLog = ApprovalActionLog.builder()
                            .approvalRequest(ar)
                            .actor(ar.getApprover())
                            .action("REJECT")
                            .message(message)
                            .build();
                    approvalActionLogRepository.save(rejectLog);
                    log.info("반려 로그 생성: ApprovalRequest ID={}", ar.getId());
                }
            } else if (ar.getStatusSnapshot() == ApprovalStatus.REQUEST_MORE_INFO && ar.getApprover() != null) {
                boolean hasRequestMoreInfoLog = approvalActionLogRepository.findByApprovalRequestIdOrderByCreatedAtAsc(ar.getId())
                        .stream()
                        .anyMatch(log -> "REQUEST_MORE_INFO".equals(log.getAction()));

                if (!hasRequestMoreInfoLog) {
                    Expense expense = expenseRepository.findById(ar.getRefId())
                            .orElse(null);
                    
                    String message = "추가 정보가 필요합니다. 지출 목적과 업무 관련성을 명확히 기재해주세요.";
                    if (expense != null) {
                        if (expense.getMerchant() != null && expense.getMerchant().contains("맥도날드")) {
                            message = "점심 식사 내역에 대해 업무 관련성 확인이 필요합니다. 회의 참석자 명단 또는 업무 관련 증빙을 추가해주세요.";
                        } else if (expense.getMerchant() != null && expense.getMerchant().contains("GS25")) {
                            message = "구매한 품목의 상세 내역이 필요합니다. 영수증에 품목명이 명확히 보이도록 재촬영해주세요.";
                        }
                    }

                    ApprovalActionLog requestMoreInfoLog = ApprovalActionLog.builder()
                            .approvalRequest(ar)
                            .actor(ar.getApprover())
                            .action("REQUEST_MORE_INFO")
                            .message(message)
                            .build();
                    approvalActionLogRepository.save(requestMoreInfoLog);
                    log.info("보완요청 로그 생성: ApprovalRequest ID={}", ar.getId());
                }
            }
        }

        log.info("결재 액션 로그 더미 데이터 생성 완료");
    }
}

