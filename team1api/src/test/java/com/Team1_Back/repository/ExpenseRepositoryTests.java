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
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Slf4j
public class ExpenseRepositoryTests {

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ApprovalRequestRepository approvalRequestRepository;

    @Autowired
    private ApprovalActionLogRepository approvalActionLogRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    @Transactional
    public void testInsert() {
        // given
        User testUser = userRepository.findByEmployeeNo("EMP00001")
                .orElseThrow(() -> new RuntimeException("테스트 사용자를 찾을 수 없습니다."));

        Expense expense = Expense.builder()
                .writer(testUser)
                .status(ApprovalStatus.DRAFT)
                .merchant("테스트 상점")
                .amount(10000)
                .category("식비")
                .receiptDate(LocalDate.now())
                .description("테스트 지출 내역")
                .build();

        // when
        Expense saved = expenseRepository.save(expense);

        // then
        assertNotNull(saved.getId());
        log.info("저장된 지출 내역 ID: {}", saved.getId());
        log.info("저장된 지출 내역: {}", saved);
    }

    @Test
    @Transactional
    public void testFindByWriterId() {
        // given
        User testUser = userRepository.findByEmployeeNo("EMP00001")
                .orElseThrow(() -> new RuntimeException("테스트 사용자를 찾을 수 없습니다."));

        Pageable pageable = PageRequest.of(0, 10, Sort.by("updatedAt").descending());

        // when
        Page<Expense> result = expenseRepository.findByWriterId(testUser.getId(), pageable);

        // then
        assertNotNull(result);
        log.info("총 개수: {}", result.getTotalElements());
        log.info("페이지 수: {}", result.getTotalPages());
        result.getContent().forEach(expense -> log.info("expense: {}", expense));
    }

    @Test
    @Transactional
    public void testFindByWriterIdAndStatus() {
        // given
        User testUser = userRepository.findByEmployeeNo("EMP00001")
                .orElseThrow(() -> new RuntimeException("테스트 사용자를 찾을 수 없습니다."));

        Pageable pageable = PageRequest.of(0, 10, Sort.by("updatedAt").descending());

        // when
        Page<Expense> result = expenseRepository.findByWriterIdAndStatus(
                testUser.getId(), 
                ApprovalStatus.APPROVED, 
                pageable
        );

        // then
        assertNotNull(result);
        log.info("APPROVED 상태 지출 내역 개수: {}", result.getTotalElements());
        result.getContent().forEach(expense -> {
            assertEquals(ApprovalStatus.APPROVED, expense.getStatus());
            log.info("expense: {}", expense);
        });
    }

    @Test
    @Transactional
    public void testFindByUserIdAndDateRange() {
        // given
        User testUser = userRepository.findByEmployeeNo("EMP00001")
                .orElseThrow(() -> new RuntimeException("테스트 사용자를 찾을 수 없습니다."));

        LocalDate startDate = LocalDate.now().minusMonths(1);
        LocalDate endDate = LocalDate.now();
        Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());

        // when
        Page<Expense> result = expenseRepository.findByUserIdAndDateRange(
                testUser.getId(),
                startDate,
                endDate,
                pageable
        );

        // then
        assertNotNull(result);
        log.info("기간별 지출 내역 개수: {}", result.getTotalElements());
        result.getContent().forEach(expense -> {
            LocalDate createdAt = expense.getCreatedAt() != null 
                ? expense.getCreatedAt().toLocalDate() 
                : null;
            
            assertNotNull(createdAt, "createdAt이 null입니다.");
            assertTrue(createdAt.isAfter(startDate.minusDays(1)) || createdAt.isEqual(startDate));
            assertTrue(createdAt.isBefore(endDate.plusDays(1)) || createdAt.isEqual(endDate));
            log.info("상신일: {}, 지출 일자: {}, 금액: {}", 
                createdAt, expense.getReceiptDate(), expense.getAmount());
        });
    }

    @Test
    @Transactional
    public void testFindByIdAndWriterId() {
        // given
        User testUser = userRepository.findByEmployeeNo("EMP00001")
                .orElseThrow(() -> new RuntimeException("테스트 사용자를 찾을 수 없습니다."));

        // 실제 존재하는 지출 ID를 사용하거나, 먼저 생성
        Expense expense = Expense.builder()
                .writer(testUser)
                .status(ApprovalStatus.DRAFT)
                .merchant("권한 테스트 상점")
                .amount(5000)
                .category("교통비")
                .receiptDate(LocalDate.now())
                .build();
        Expense saved = expenseRepository.save(expense);

        // when
        Optional<Expense> result = expenseRepository.findByIdAndWriterId(
                saved.getId(),
                testUser.getId()
        );

        // then
        assertTrue(result.isPresent());
        assertEquals(saved.getId(), result.get().getId());
        assertEquals(testUser.getId(), result.get().getWriter().getId());
        log.info("조회된 지출 내역: {}", result.get());
    }

    @Test
    @Transactional
    public void testFindByIdWithWriter() {
        // given
        User testUser = userRepository.findByEmployeeNo("EMP00001")
                .orElseThrow(() -> new RuntimeException("테스트 사용자를 찾을 수 없습니다."));

        Expense expense = Expense.builder()
                .writer(testUser)
                .status(ApprovalStatus.DRAFT)
                .merchant("관리자 조회 테스트")
                .amount(15000)
                .category("비품")
                .receiptDate(LocalDate.now())
                .build();
        Expense saved = expenseRepository.save(expense);

        // when
        Optional<Expense> result = expenseRepository.findByIdWithWriter(saved.getId());

        // then
        assertTrue(result.isPresent());
        assertNotNull(result.get().getWriter());
        assertEquals(testUser.getId(), result.get().getWriter().getId());
        log.info("관리자 조회 결과: {}", result.get());
        log.info("작성자 정보: {}", result.get().getWriter());
    }

    @Test
    @Transactional
    public void testFindByStatus() {
        // given
        Pageable pageable = PageRequest.of(0, 10, Sort.by("updatedAt").descending());

        // when
        Page<Expense> result = expenseRepository.findByStatus(ApprovalStatus.DRAFT, pageable);

        // then
        assertNotNull(result);
        log.info("DRAFT 상태 지출 내역 개수: {}", result.getTotalElements());
        result.getContent().forEach(expense -> {
            assertEquals(ApprovalStatus.DRAFT, expense.getStatus());
            log.info("expense: {}", expense);
        });
    }

    @Test
    @Transactional
    public void testFindDepartmentStatistics() {
        // when
        List<Object[]> results = expenseRepository.findDepartmentStatistics("APPROVED");

        // then
        assertNotNull(results);
        log.info("부서별 통계 결과 개수: {}", results.size());
        results.forEach(row -> {
            String departmentName = (String) row[0];
            Long expenseCount = ((Number) row[1]).longValue();
            Long totalAmount = ((Number) row[2]).longValue();
            log.info("부서: {}, 건수: {}, 총액: {}", departmentName, expenseCount, totalAmount);
        });
    }

    @Test
    @Transactional
    public void testFindCategoryStatistics() {
        // when
        List<Object[]> results = expenseRepository.findCategoryStatistics("APPROVED");

        // then
        assertNotNull(results);
        log.info("카테고리별 통계 결과 개수: {}", results.size());
        results.forEach(row -> {
            String categoryName = (String) row[0];
            Long expenseCount = ((Number) row[1]).longValue();
            Long totalAmount = ((Number) row[2]).longValue();
            log.info("카테고리: {}, 건수: {}, 총액: {}", categoryName, expenseCount, totalAmount);
        });
    }

    @Test
    @Transactional
    public void testSumMonthlyTotalExpense() {
        // when
        Long total = expenseRepository.sumMonthlyTotalExpense("APPROVED");

        // then
        assertNotNull(total);
        assertTrue(total >= 0);
        log.info("이번 달 총 지출액 (APPROVED): {}", total);
    }

    @Test
    @Transactional
    public void testFindForReportByDateRange() {
        // given
        LocalDate startDate = LocalDate.now().minusMonths(1);
        LocalDate endDate = LocalDate.now();

        // when
        List<Expense> results = expenseRepository.findForReportByDateRange(startDate, endDate);

        // then
        assertNotNull(results);
        log.info("리포트용 지출 내역 개수: {}", results.size());
        results.forEach(expense -> {
            assertEquals(ApprovalStatus.APPROVED, expense.getStatus());
            assertTrue(expense.getReceiptDate().isAfter(startDate.minusDays(1)));
            assertTrue(expense.getReceiptDate().isBefore(endDate.plusDays(1)));
            log.info("리포트 항목: {}", expense);
        });
    }

    // ========== 더미 데이터 생성 메서드 ==========

    /**
     * 지출 내역 더미 데이터 생성
     * 
     * ⚠️ 사전 요구사항:
     * - HR 팀원이 UserDataInitTest.insertTestUsers() 먼저 실행해야 함
     * - 최소 4명의 사용자 (EMP00001, EMP00002, EMP00003, EMP00004) 필요
     * 
     * 생성 개수:
     * - user1: 100개 (페이징 테스트용, 예산 초과 주의 인원 아님)
     * - user2: 10개 (예산 초과 주의 인원용, 현재 월 APPROVED 약 170만원)
     * - user3: 10개 (예산 초과 주의 인원용, 현재 월 APPROVED 약 180만원)
     * - user4: 10개 (정상)
     */
    @Test
    @Transactional
    @Rollback(false)  
    public void insertDummyExpenses() {
        // 필요한 사용자들
        User user1 = userRepository.findByEmployeeNo("EMP00001")
                .orElseThrow(() -> new RuntimeException("EMP00001 사용자를 찾을 수 없습니다. UserDataInitTest.insertTestUsers()를 먼저 실행하세요."));
        User user2 = userRepository.findByEmployeeNo("EMP00002")
                .orElseThrow(() -> new RuntimeException("EMP00002 사용자를 찾을 수 없습니다. UserDataInitTest.insertTestUsers()를 먼저 실행하세요."));
        User user3 = userRepository.findByEmployeeNo("EMP00003")
                .orElseThrow(() -> new RuntimeException("EMP00003 사용자를 찾을 수 없습니다. UserDataInitTest.insertTestUsers()를 먼저 실행하세요."));
        User user4 = userRepository.findByEmployeeNo("EMP00004")
                .orElseThrow(() -> new RuntimeException("EMP00004 사용자를 찾을 수 없습니다. UserDataInitTest.insertTestUsers()를 먼저 실행하세요."));

        // ✅ 가맹점 확장 및 분류
        String[] smallMerchants = {"스타벅스 강남점", "맥도날드 역삼점", "GS25 편의점", "CU 편의점", "세븐일레븐", "이디야커피 강남점", "던킨도넛 역삼점"};
        String[] restaurantMerchants = {"맥도날드 역삼점", "롯데리아 강남점", "한솥도시락", "본죽", "김밥천국"};
        String[] largeMerchants = {"이마트", "롯데마트", "홈플러스", "오피스플러스", "스테이션러너", "교보문고", "반디앤루니스"};
        
        String[] categories = {"식비", "비품", "교통비", "기타"};
        // ✅ 과거 데이터는 승인/반려만
        ApprovalStatus[] pastStatuses = {ApprovalStatus.APPROVED, ApprovalStatus.REJECTED};
        
        Random random = new Random();
        LocalDate now = LocalDate.now();
        LocalDate currentMonthStart = now.withDayOfMonth(1); // 현재 월 1일
        
        // 전월 날짜 계산 (전월 대비 비교용)
        LocalDate lastMonth = now.minusMonths(1);
        LocalDate lastMonthStart = lastMonth.withDayOfMonth(1);
        int lastMonthDays = lastMonth.lengthOfMonth();
        
        // ========== user1: 100개 생성 (페이징 테스트용, 예산 초과 주의 인원 아님) ==========
        // 현재 월 APPROVED: 20개 (약 50만원, 200만원 예산의 25% 소진율) - 통계용으로 유지
        int targetCurrentMonthExpense = 500000; // 200만원 예산의 25%
        int currentExpense = 0;
        
        for (int i = 0; i < 20; i++) {
            // ✅ 고액은 대형마트에서만
            String merchant = largeMerchants[random.nextInt(largeMerchants.length)];
            int amount = getAmountByMerchant(merchant, random);
            if (currentExpense + amount > targetCurrentMonthExpense) {
                amount = targetCurrentMonthExpense - currentExpense;
            }
            if (amount <= 0) break;
            
            createExpense(
                user1,
                ApprovalStatus.APPROVED,
                merchant,
                amount,
                categories[random.nextInt(categories.length)],
                currentMonthStart.plusDays(random.nextInt(Math.max(1, now.getDayOfMonth() - 1))),
                "더미 지출 내역 (현재 월 APPROVED) " + (i + 1)
            );
            currentExpense += amount;
        }
        
        // 현재 월 DRAFT: 5개 (DRAFT는 통계에 영향 없음)
        for (int i = 0; i < 5; i++) {
            String[] allMerchants = {"스타벅스 강남점", "GS25 편의점", "맥도날드 역삼점", "이마트"};
            String merchant = allMerchants[random.nextInt(allMerchants.length)];
            int amount = getAmountByMerchant(merchant, random);
            
            createExpense(
                user1,
                ApprovalStatus.DRAFT,
                merchant,
                amount,
                categories[random.nextInt(categories.length)],
                currentMonthStart.plusDays(random.nextInt(Math.max(1, now.getDayOfMonth() - 1))),
                "더미 지출 내역 (현재 월 DRAFT) " + (i + 1)
            );
        }
        
        // ✅ 전월 APPROVED 데이터: 20개 (현재 월과 비슷한 수준, 전월 대비 비교용)
        int targetLastMonthExpense = 450000; // 현재 월(50만원)보다 약간 적게
        int lastMonthExpense = 0;
        
        for (int i = 0; i < 20; i++) {
            String merchant = largeMerchants[random.nextInt(largeMerchants.length)];
            int amount = getAmountByMerchant(merchant, random);
            if (lastMonthExpense + amount > targetLastMonthExpense) {
                amount = targetLastMonthExpense - lastMonthExpense;
            }
            if (amount <= 0) break;
            
            createExpense(
                user1,
                ApprovalStatus.APPROVED,
                merchant,
                amount,
                categories[random.nextInt(categories.length)],
                lastMonthStart.plusDays(random.nextInt(lastMonthDays)),
                "더미 지출 내역 (전월 APPROVED) " + (i + 1)
            );
            lastMonthExpense += amount;
        }
        
        // ✅ 과거 월 데이터: 55개 (승인/반려만, 60-180일 전) - 페이징 테스트용 (전월 제외)
        for (int i = 0; i < 55; i++) {
            ApprovalStatus status = pastStatuses[random.nextInt(pastStatuses.length)];
            
            // 가맹점 랜덤 선택
            String merchant;
            int merchantType = random.nextInt(3);
            if (merchantType == 0) {
                merchant = smallMerchants[random.nextInt(smallMerchants.length)];
            } else if (merchantType == 1) {
                merchant = restaurantMerchants[random.nextInt(restaurantMerchants.length)];
            } else {
                merchant = largeMerchants[random.nextInt(largeMerchants.length)];
            }
            
            int amount = getAmountByMerchant(merchant, random);
            
            createExpense(
                user1,
                status,
                merchant,
                amount,
                categories[random.nextInt(categories.length)],
                now.minusDays(60 + random.nextInt(120)), // 60-180일 전 (전월 제외)
                "더미 지출 내역 (과거) " + (i + 1)
            );
        }
        
        // ========== user2: 10개 (예산 초과 주의 인원용) ==========
        // 현재 월 APPROVED: 8개 (약 170만원, 200만원 예산의 85% 소진율 목표)
        int targetExpense2 = 1700000;
        int currentExpense2 = 0;
        
        for (int i = 0; i < 8; i++) {
            // ✅ 고액은 대형마트에서만
            String merchant = largeMerchants[random.nextInt(largeMerchants.length)];
            int amount = 150000 + random.nextInt(100000); // 15만원~25만원
            if (currentExpense2 + amount > targetExpense2) {
                amount = targetExpense2 - currentExpense2;
            }
            if (amount <= 0) break;
            
            createExpense(
                user2,
                ApprovalStatus.APPROVED,
                merchant,
                amount,
                categories[random.nextInt(categories.length)],
                currentMonthStart.plusDays(random.nextInt(Math.max(1, now.getDayOfMonth() - 1))),
                "더미 지출 내역 user2 (현재 월 APPROVED) " + (i + 1)
            );
            currentExpense2 += amount;
        }
        
        // ✅ 전월 APPROVED 데이터: 8개 (현재 월과 비슷한 수준)
        int targetLastMonthExpense2 = 1600000; // 현재 월(170만원)보다 약간 적게
        int lastMonthExpense2 = 0;
        
        for (int i = 0; i < 8; i++) {
            String merchant = largeMerchants[random.nextInt(largeMerchants.length)];
            int amount = 150000 + random.nextInt(100000);
            if (lastMonthExpense2 + amount > targetLastMonthExpense2) {
                amount = targetLastMonthExpense2 - lastMonthExpense2;
            }
            if (amount <= 0) break;
            
            createExpense(
                user2,
                ApprovalStatus.APPROVED,
                merchant,
                amount,
                categories[random.nextInt(categories.length)],
                lastMonthStart.plusDays(random.nextInt(lastMonthDays)),
                "더미 지출 내역 user2 (전월 APPROVED) " + (i + 1)
            );
            lastMonthExpense2 += amount;
        }
        
        // ========== user3: 10개 (예산 초과 주의 인원용) ==========
        // 현재 월 APPROVED: 8개 (약 180만원, 200만원 예산의 90% 소진율 목표)
        int targetExpense3 = 1800000;
        int currentExpense3 = 0;
        
        for (int i = 0; i < 8; i++) {
            String merchant = largeMerchants[random.nextInt(largeMerchants.length)];
            int amount = 150000 + random.nextInt(100000);
            if (currentExpense3 + amount > targetExpense3) {
                amount = targetExpense3 - currentExpense3;
            }
            if (amount <= 0) break;
            
            createExpense(
                user3,
                ApprovalStatus.APPROVED,
                merchant,
                amount,
                categories[random.nextInt(categories.length)],
                currentMonthStart.plusDays(random.nextInt(Math.max(1, now.getDayOfMonth() - 1))),
                "더미 지출 내역 user3 (현재 월 APPROVED) " + (i + 1)
            );
            currentExpense3 += amount;
        }
        
        // ✅ 전월 APPROVED 데이터: 8개 (현재 월과 비슷한 수준)
        int targetLastMonthExpense3 = 1700000; // 현재 월(180만원)보다 약간 적게
        int lastMonthExpense3 = 0;
        
        for (int i = 0; i < 8; i++) {
            String merchant = largeMerchants[random.nextInt(largeMerchants.length)];
            int amount = 150000 + random.nextInt(100000);
            if (lastMonthExpense3 + amount > targetLastMonthExpense3) {
                amount = targetLastMonthExpense3 - lastMonthExpense3;
            }
            if (amount <= 0) break;
            
            createExpense(
                user3,
                ApprovalStatus.APPROVED,
                merchant,
                amount,
                categories[random.nextInt(categories.length)],
                lastMonthStart.plusDays(random.nextInt(lastMonthDays)),
                "더미 지출 내역 user3 (전월 APPROVED) " + (i + 1)
            );
            lastMonthExpense3 += amount;
        }
        
        // ========== user4: 10개 (정상 예산) ==========
        // 전월 APPROVED 데이터: 5개 (현재 월과 비슷한 수준)
        for (int i = 0; i < 5; i++) {
            String merchant;
            int merchantType = random.nextInt(3);
            if (merchantType == 0) {
                merchant = smallMerchants[random.nextInt(smallMerchants.length)];
            } else if (merchantType == 1) {
                merchant = restaurantMerchants[random.nextInt(restaurantMerchants.length)];
            } else {
                merchant = largeMerchants[random.nextInt(largeMerchants.length)];
            }
            int amount = getAmountByMerchant(merchant, random);
            
            createExpense(
                user4,
                ApprovalStatus.APPROVED,
                merchant,
                amount,
                categories[random.nextInt(categories.length)],
                lastMonthStart.plusDays(random.nextInt(lastMonthDays)),
                "더미 지출 내역 user4 (전월 APPROVED) " + (i + 1)
            );
        }
        
        // 나머지 5개는 과거 데이터 (60일 이상 전)
        for (int i = 0; i < 5; i++) {
            ApprovalStatus status = pastStatuses[random.nextInt(pastStatuses.length)];
            String merchant;
            int merchantType = random.nextInt(3);
            if (merchantType == 0) {
                merchant = smallMerchants[random.nextInt(smallMerchants.length)];
            } else if (merchantType == 1) {
                merchant = restaurantMerchants[random.nextInt(restaurantMerchants.length)];
            } else {
                merchant = largeMerchants[random.nextInt(largeMerchants.length)];
            }
            int amount = getAmountByMerchant(merchant, random);
            
            createExpense(
                user4,
                status,
                merchant,
                amount,
                categories[random.nextInt(categories.length)],
                now.minusDays(60 + random.nextInt(120)), // 60-180일 전
                "더미 지출 내역 user4 (과거) " + (i + 1)
            );
        }
        
        log.info("지출 내역 더미 데이터 생성 완료: user1(100개), user2(16개), user3(16개), user4(10개)");
        log.info("user1 현재 월 APPROVED 지출: 약 {}원, 전월 APPROVED 지출: 약 {}원", currentExpense, lastMonthExpense);
        log.info("user2 현재 월 APPROVED 지출: 약 {}원, 전월 APPROVED 지출: 약 {}원", currentExpense2, lastMonthExpense2);
        log.info("user3 현재 월 APPROVED 지출: 약 {}원, 전월 APPROVED 지출: 약 {}원", currentExpense3, lastMonthExpense3);
    }

    /**
     * 통계용 현재 월 APPROVED 지출 내역 추가
     * ExpenseRepositoryTests.insertDummyExpenses() 실행 후 사용
     */
    @Test
    @Transactional  
    @Rollback(false)
    public void insertDummyExpensesForStatistics() {
        User user1 = userRepository.findByEmployeeNo("EMP00001")
                .orElseThrow(() -> new RuntimeException("EMP00001 사용자를 찾을 수 없습니다."));
        User user2 = userRepository.findByEmployeeNo("EMP00002")
                .orElseThrow(() -> new RuntimeException("EMP00002 사용자를 찾을 수 없습니다."));
        User user3 = userRepository.findByEmployeeNo("EMP00003")
                .orElseThrow(() -> new RuntimeException("EMP00003 사용자를 찾을 수 없습니다."));
        User user4 = userRepository.findByEmployeeNo("EMP00004")
                .orElseThrow(() -> new RuntimeException("EMP00004 사용자를 찾을 수 없습니다."));

        LocalDate now = LocalDate.now();

        // 사용자 (EMP00001, EMP00003) - 현재 월 APPROVED
        createExpense(user1, ApprovalStatus.APPROVED, "이마트", 150000, "비품", now.minusDays(1), "개발팀 사무용품 구매");
        createExpense(user1, ApprovalStatus.APPROVED, "스타벅스 강남점", 15000, "식비", now.minusDays(3), "팀 회의");
        createExpense(user3, ApprovalStatus.APPROVED, "이마트", 616000, "비품", now.minusDays(5), "마케팅팀 홍보물");
        createExpense(user3, ApprovalStatus.APPROVED, "스타벅스 강남점", 18000, "식비", now.minusDays(1), "팀 회의");

        // 사용자 (EMP00002, EMP00004) - 현재 월 APPROVED
        createExpense(user2, ApprovalStatus.APPROVED, "이마트", 643000, "비품", now.minusDays(4), "영업팀 프레젠테이션 용품");
        createExpense(user2, ApprovalStatus.APPROVED, "스타벅스 강남점", 12000, "식비", now.minusDays(2), "고객 미팅");
        createExpense(user4, ApprovalStatus.APPROVED, "교보문고", 80000, "비품", now.minusDays(2), "기술 서적 구매");
        createExpense(user4, ApprovalStatus.APPROVED, "맥도날드 역삼점", 15000, "식비", now.minusDays(1), "점심 식사");

        // 카테고리별 통계용 추가 데이터
        createExpense(user1, ApprovalStatus.APPROVED, "GS25 편의점", 8000, "기타", now.minusDays(6), "기타 지출");
        createExpense(user2, ApprovalStatus.APPROVED, "GS25 편의점", 12000, "기타", now.minusDays(3), "기타 지출");
        createExpense(user3, ApprovalStatus.APPROVED, "GS25 편의점", 15000, "기타", now.minusDays(2), "기타 지출");

        log.info("통계용 지출 내역 더미 데이터 생성 완료");
    }

    /**
     * 과거 2-3개월치 지출 내역 데이터 추가 (회계 통계용)
     * ExpenseRepositoryTests.insertDummyExpenses() 실행 후 사용
     */
    @Test
    @Transactional  
    @Rollback(false)
    public void insertDummyHistoricalExpenses() {
        User user1 = userRepository.findByEmployeeNo("EMP00001")
                .orElseThrow(() -> new RuntimeException("EMP00001 사용자를 찾을 수 없습니다."));
        User user2 = userRepository.findByEmployeeNo("EMP00003")
                .orElseThrow(() -> new RuntimeException("EMP00003 사용자를 찾을 수 없습니다."));
        User user3 = userRepository.findByEmployeeNo("EMP00005")
                .orElseThrow(() -> new RuntimeException("EMP00005 사용자를 찾을 수 없습니다."));
        User user4 = userRepository.findByEmployeeNo("EMP00007")
                .orElseThrow(() -> new RuntimeException("EMP00007 사용자를 찾을 수 없습니다."));

        LocalDate now = LocalDate.now();
        Random random = new Random();
        ApprovalStatus[] pastStatuses = {ApprovalStatus.APPROVED, ApprovalStatus.REJECTED};

        // EMP00001 - 2개월 전 데이터
        createExpense(user1, pastStatuses[random.nextInt(pastStatuses.length)], "스타벅스 강남점", 15000, "식비", now.minusDays(60), "팀 회의 커피");
        createExpense(user1, pastStatuses[random.nextInt(pastStatuses.length)], "이마트", 45000, "비품", now.minusDays(55), "사무용품 구매");
        createExpense(user1, pastStatuses[random.nextInt(pastStatuses.length)], "GS25 편의점", 8000, "기타", now.minusDays(50), "간식 구매");
        createExpense(user1, pastStatuses[random.nextInt(pastStatuses.length)], "교보문고", 35000, "비품", now.minusDays(45), "기술 서적");
        createExpense(user1, pastStatuses[random.nextInt(pastStatuses.length)], "스타벅스 강남점", 18000, "식비", now.minusDays(40), "고객 미팅");

        // EMP00001 - 1개월 전 데이터
        createExpense(user1, pastStatuses[random.nextInt(pastStatuses.length)], "이마트", 52000, "비품", now.minusDays(35), "개발 도구");
        createExpense(user1, pastStatuses[random.nextInt(pastStatuses.length)], "맥도날드 역삼점", 12000, "식비", now.minusDays(30), "점심 식사");
        createExpense(user1, pastStatuses[random.nextInt(pastStatuses.length)], "GS25 편의점", 6000, "기타", now.minusDays(28), "음료 구매");
        createExpense(user1, pastStatuses[random.nextInt(pastStatuses.length)], "스타벅스 강남점", 20000, "식비", now.minusDays(22), "코드 리뷰 미팅");
        createExpense(user1, pastStatuses[random.nextInt(pastStatuses.length)], "교보문고", 28000, "비품", now.minusDays(20), "도서 구매");

        // EMP00003 - 2개월 전 데이터
        createExpense(user2, pastStatuses[random.nextInt(pastStatuses.length)], "스타벅스 강남점", 25000, "식비", now.minusDays(58), "고객 미팅");
        createExpense(user2, pastStatuses[random.nextInt(pastStatuses.length)], "이마트", 68000, "비품", now.minusDays(52), "프레젠테이션 용품");
        createExpense(user2, pastStatuses[random.nextInt(pastStatuses.length)], "GS25 편의점", 5000, "기타", now.minusDays(48), "음료 구매");
        createExpense(user2, pastStatuses[random.nextInt(pastStatuses.length)], "교보문고", 42000, "비품", now.minusDays(42), "업무 서적");
        createExpense(user2, pastStatuses[random.nextInt(pastStatuses.length)], "스타벅스 강남점", 22000, "식비", now.minusDays(38), "고객 미팅");

        // EMP00003 - 1개월 전 데이터
        createExpense(user2, pastStatuses[random.nextInt(pastStatuses.length)], "이마트", 55000, "비품", now.minusDays(32), "마케팅 자료");
        createExpense(user2, pastStatuses[random.nextInt(pastStatuses.length)], "맥도날드 역삼점", 10000, "식비", now.minusDays(27), "점심 식사");
        createExpense(user2, pastStatuses[random.nextInt(pastStatuses.length)], "GS25 편의점", 4000, "기타", now.minusDays(24), "간식 구매");
        createExpense(user2, pastStatuses[random.nextInt(pastStatuses.length)], "스타벅스 강남점", 30000, "식비", now.minusDays(19), "고객 미팅");
        createExpense(user2, pastStatuses[random.nextInt(pastStatuses.length)], "교보문고", 38000, "비품", now.minusDays(16), "업무 서적");

        // EMP00005 - 2개월 전 데이터
        createExpense(user3, pastStatuses[random.nextInt(pastStatuses.length)], "스타벅스 강남점", 18000, "식비", now.minusDays(56), "팀 회의");
        createExpense(user3, pastStatuses[random.nextInt(pastStatuses.length)], "이마트", 75000, "비품", now.minusDays(50), "마케팅 용품");
        createExpense(user3, pastStatuses[random.nextInt(pastStatuses.length)], "GS25 편의점", 9000, "기타", now.minusDays(46), "간식 구매");
        createExpense(user3, pastStatuses[random.nextInt(pastStatuses.length)], "교보문고", 48000, "비품", now.minusDays(40), "마케팅 도서");
        createExpense(user3, pastStatuses[random.nextInt(pastStatuses.length)], "스타벅스 강남점", 16000, "식비", now.minusDays(36), "팀 회의");

        // EMP00005 - 1개월 전 데이터
        createExpense(user3, pastStatuses[random.nextInt(pastStatuses.length)], "이마트", 62000, "비품", now.minusDays(33), "마케팅 자료");
        createExpense(user3, pastStatuses[random.nextInt(pastStatuses.length)], "맥도날드 역삼점", 13000, "식비", now.minusDays(29), "점심 식사");
        createExpense(user3, pastStatuses[random.nextInt(pastStatuses.length)], "GS25 편의점", 7000, "기타", now.minusDays(26), "음료 구매");
        createExpense(user3, pastStatuses[random.nextInt(pastStatuses.length)], "스타벅스 강남점", 24000, "식비", now.minusDays(21), "팀 회의");
        createExpense(user3, pastStatuses[random.nextInt(pastStatuses.length)], "교보문고", 36000, "비품", now.minusDays(17), "마케팅 도서");

        // EMP00007 - 2개월 전 데이터
        createExpense(user4, pastStatuses[random.nextInt(pastStatuses.length)], "스타벅스 강남점", 14000, "식비", now.minusDays(54), "코드 리뷰 미팅");
        createExpense(user4, pastStatuses[random.nextInt(pastStatuses.length)], "이마트", 38000, "비품", now.minusDays(48), "개발 도구");
        createExpense(user4, pastStatuses[random.nextInt(pastStatuses.length)], "GS25 편의점", 6500, "기타", now.minusDays(44), "음료 구매");
        createExpense(user4, pastStatuses[random.nextInt(pastStatuses.length)], "교보문고", 32000, "비품", now.minusDays(38), "기술 서적");
        createExpense(user4, pastStatuses[random.nextInt(pastStatuses.length)], "스타벅스 강남점", 17000, "식비", now.minusDays(34), "코드 리뷰 미팅");

        // EMP00007 - 1개월 전 데이터
        createExpense(user4, pastStatuses[random.nextInt(pastStatuses.length)], "이마트", 48000, "비품", now.minusDays(31), "개발 도구");
        createExpense(user4, pastStatuses[random.nextInt(pastStatuses.length)], "맥도날드 역삼점", 11000, "식비", now.minusDays(28), "점심 식사");
        createExpense(user4, pastStatuses[random.nextInt(pastStatuses.length)], "GS25 편의점", 5500, "기타", now.minusDays(25), "간식 구매");
        createExpense(user4, pastStatuses[random.nextInt(pastStatuses.length)], "스타벅스 강남점", 19000, "식비", now.minusDays(23), "코드 리뷰 미팅");
        createExpense(user4, pastStatuses[random.nextInt(pastStatuses.length)], "교보문고", 29000, "비품", now.minusDays(19), "기술 서적");

        log.info("과거 지출 내역 더미 데이터 생성 완료");
    }

    /**
     * 가맹점별 현실적인 금액 생성 헬퍼 메서드
     */
    private int getAmountByMerchant(String merchant, Random random) {
        // 소액 가맹점 (편의점, 커피숍)
        String[] smallKeywords = {"스타벅스", "이디야", "던킨", "GS25", "CU", "세븐일레븐"};
        for (String keyword : smallKeywords) {
            if (merchant.contains(keyword)) {
                return 5000 + random.nextInt(25000); // 5천원~3만원
            }
        }
        
        // 일반 식당
        String[] restaurantKeywords = {"맥도날드", "롯데리아", "한솥", "본죽", "김밥"};
        for (String keyword : restaurantKeywords) {
            if (merchant.contains(keyword)) {
                return 10000 + random.nextInt(40000); // 1만원~5만원
            }
        }
        
        // 대형마트/백화점/오피스용품점 (고액 가능)
        String[] largeKeywords = {"이마트", "롯데마트", "홈플러스", "오피스", "스테이션", "교보문고", "반디"};
        for (String keyword : largeKeywords) {
            if (merchant.contains(keyword)) {
                return 50000 + random.nextInt(950000); // 5만원~100만원
            }
        }
        
        // 기본값 (중간 범위)
        return 10000 + random.nextInt(90000);
    }

    private void createExpense(User user, ApprovalStatus status, String merchant, int amount, 
                               String category, LocalDate receiptDate, String description) {
        Expense expense = Expense.builder()
                .writer(user)
                .status(status)
                .merchant(merchant)
                .amount(amount)
                .category(category)
                .receiptDate(receiptDate)
                .description(description)
                .build();
        Expense savedExpense = expenseRepository.save(expense);
        
        // ✅ 상신일(createdAt)을 receiptDate와 비슷한 시점으로 설정
        // Native Query로 직접 DB 업데이트하여 @CreationTimestamp/@UpdateTimestamp 우회
        // 상신일은 보통 지출일자와 같거나 약간 늦음 (같은 날 오전 9시~오후 5시 사이)
        // ⚠️ 오늘 날짜는 제외: receiptDate가 오늘이면 어제 날짜로 변경
        LocalDate dateForCreatedAt = receiptDate;
        if (receiptDate.equals(LocalDate.now())) {
            dateForCreatedAt = receiptDate.minusDays(1);
        }
        Random random = new Random();
        LocalDateTime createdAt = dateForCreatedAt.atStartOfDay()
                .plusHours(9 + random.nextInt(8)); // 오전 9시~오후 5시 사이
        
        // Native Query로 직접 UPDATE (테스트 코드에서만 사용)
        String updateQuery = "UPDATE expense SET created_at = :createdAt, updated_at = :updatedAt WHERE id = :id";
        entityManager.createNativeQuery(updateQuery)
                .setParameter("createdAt", createdAt)
                .setParameter("updatedAt", createdAt)
                .setParameter("id", savedExpense.getId())
                .executeUpdate();
        
        // EntityManager의 변경사항을 플러시하여 즉시 반영
        entityManager.flush();
        
        // ✅ 승인/반려된 지출 내역에 대해 결재 이력 자동 생성
        if (status == ApprovalStatus.APPROVED || status == ApprovalStatus.REJECTED) {
            createApprovalHistory(savedExpense, createdAt, status, random);
        }
        
        entityManager.clear(); // 영속성 컨텍스트 초기화하여 다음 조회 시 DB에서 다시 읽어오도록
    }

    /**
     * 결재 이력 생성 헬퍼 메서드
     * ApprovalRequest와 ApprovalActionLog를 생성합니다.
     */
    private void createApprovalHistory(Expense expense, LocalDateTime createdAt, ApprovalStatus status, Random random) {
        // 관리자 조회 (approver로 사용)
        User admin = userRepository.findByEmployeeNo("EMP00002")
                .orElseGet(() -> {
                    // EMP00002가 없으면 첫 번째 관리자 찾기
                    return userRepository.findAll().stream()
                            .filter(User::isAdmin)
                            .findFirst()
                            .orElse(expense.getWriter()); // 관리자가 없으면 작성자 사용
                });

        // ApprovalRequest 생성
        ApprovalRequest approvalRequest = ApprovalRequest.builder()
                .requestType("EXPENSE")
                .refId(expense.getId())
                .requester(expense.getWriter())
                .approver(admin)
                .statusSnapshot(status)
                .build();
        ApprovalRequest savedRequest = approvalRequestRepository.save(approvalRequest);
        
        // ApprovalRequest의 createdAt을 Expense의 createdAt과 동일하게 설정
        String updateRequestQuery = "UPDATE approval_request SET created_at = :createdAt, updated_at = :updatedAt WHERE id = :id";
        entityManager.createNativeQuery(updateRequestQuery)
                .setParameter("createdAt", createdAt)
                .setParameter("updatedAt", createdAt)
                .setParameter("id", savedRequest.getId())
                .executeUpdate();
        
        // 제출 로그 생성 (SUBMIT)
        LocalDateTime submitTime = createdAt;
        ApprovalActionLog submitLog = ApprovalActionLog.builder()
                .approvalRequest(savedRequest)
                .actor(expense.getWriter())
                .action("SUBMIT")
                .message("지출 내역을 제출했습니다.")
                .build();
        ApprovalActionLog savedSubmitLog = approvalActionLogRepository.save(submitLog);
        
        // 제출 로그의 createdAt 설정
        String updateSubmitLogQuery = "UPDATE approval_action_log SET created_at = :createdAt, updated_at = :updatedAt WHERE id = :id";
        entityManager.createNativeQuery(updateSubmitLogQuery)
                .setParameter("createdAt", submitTime)
                .setParameter("updatedAt", submitTime)
                .setParameter("id", savedSubmitLog.getId())
                .executeUpdate();
        
        // 승인/반려 로그 생성
        LocalDateTime actionTime = createdAt.plusHours(1 + random.nextInt(8)); // 제출 후 1~8시간 후
        
        String action;
        String message;
        
        if (status == ApprovalStatus.APPROVED) {
            action = "APPROVE";
            // 승인은 사유가 선택사항이지만, 테스트 데이터에는 간단한 메시지 설정
            message = "지출 내역을 승인했습니다.";
        } else { // REJECTED
            action = "REJECT";
            // ✅ 반려 사유는 필수이므로 항상 적절한 사유를 설정
            message = generateRejectReason(expense, random);
        }
        
        ApprovalActionLog actionLog = ApprovalActionLog.builder()
                .approvalRequest(savedRequest)
                .actor(admin)
                .action(action)
                .message(message) // 반려 시 필수 사유가 항상 설정됨
                .build();
        ApprovalActionLog savedActionLog = approvalActionLogRepository.save(actionLog);
        
        // 승인/반려 로그의 createdAt 설정
        String updateActionLogQuery = "UPDATE approval_action_log SET created_at = :createdAt, updated_at = :updatedAt WHERE id = :id";
        entityManager.createNativeQuery(updateActionLogQuery)
                .setParameter("createdAt", actionTime)
                .setParameter("updatedAt", actionTime)
                .setParameter("id", savedActionLog.getId())
                .executeUpdate();
        
        // ApprovalRequest의 updatedAt을 액션 시간으로 업데이트
        String updateRequestUpdatedQuery = "UPDATE approval_request SET updated_at = :updatedAt WHERE id = :id";
        entityManager.createNativeQuery(updateRequestUpdatedQuery)
                .setParameter("updatedAt", actionTime)
                .setParameter("id", savedRequest.getId())
                .executeUpdate();
        
        entityManager.flush();
        
        log.debug("결재 이력 생성 완료: Expense ID={}, Status={}, ApprovalRequest ID={}", 
                expense.getId(), status, savedRequest.getId());
    }

    /**
     * 반려 사유 생성 헬퍼 메서드
     * 지출 내역의 특성에 따라 적절한 반려 사유를 생성합니다.
     */
    private String generateRejectReason(Expense expense, Random random) {
        // 가맹점과 금액에 따라 다양한 반려 사유 생성
        if (expense.getMerchant() != null) {
            if (expense.getMerchant().contains("맥도날드") || expense.getMerchant().contains("롯데리아")) {
                return "개인 용도의 지출로 판단되어 반려합니다. 업무 관련 지출만 승인 가능합니다.";
            }
            
            if (expense.getMerchant().contains("이마트") || expense.getMerchant().contains("롯데마트") || 
                expense.getMerchant().contains("홈플러스")) {
                if (expense.getAmount() > 40000) {
                    return "금액이 과도하여 추가 확인이 필요합니다. 구매 내역서를 첨부해주세요.";
                }
            }
            
            if (expense.getMerchant().contains("스타벅스") || expense.getMerchant().contains("이디야")) {
                if (expense.getAmount() > 20000) {
                    return "회의 비용으로 과도한 금액입니다. 참석 인원 및 회의 내용을 명시해주세요.";
                }
            }
        }
        
        // 금액 기반 반려 사유
        if (expense.getAmount() > 100000) {
            return "고액 지출 내역입니다. 구매 목적 및 사용 계획서를 첨부해주세요.";
        }
        
        // 카테고리 기반 반려 사유
        if (expense.getCategory() != null) {
            if ("기타".equals(expense.getCategory())) {
                return "카테고리가 불명확합니다. 구체적인 지출 목적을 명시해주세요.";
            }
        }
        
        // 기본 반려 사유 (랜덤하게 선택)
        String[] defaultReasons = {
            "지출 내역이 명세서 기준에 부합하지 않아 반려합니다. 재제출 시 보완 부탁드립니다.",
            "영수증 정보가 불명확합니다. 명확한 영수증을 첨부해주세요.",
            "업무 관련성에 대한 설명이 부족합니다. 지출 목적을 구체적으로 기재해주세요.",
            "지출 일자와 상신 일자가 불일치합니다. 확인 후 재제출 부탁드립니다."
        };
        
        return defaultReasons[random.nextInt(defaultReasons.length)];
    }
}

