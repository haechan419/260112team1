package com.Team1_Back.repository;

import com.Team1_Back.domain.Expense;
import com.Team1_Back.domain.User;
import com.Team1_Back.domain.UserBudgetMonthly;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@SpringBootTest
@Slf4j
public class UserBudgetMonthlyRepositoryTests {

    @Autowired
    private UserBudgetMonthlyRepository userBudgetMonthlyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    // ========== 더미 데이터 생성 메서드 ==========

    /**
     * 사용자별 월간 예산 더미 데이터 생성 (현재 월만)
     * UserRepositoryTests.insertDummyUsers() 실행 후 사용
     * 
     * ⚠️ 사전 요구사항:
     * - HR 팀원이 UserRepositoryTests.insertDummyUsers() 먼저 실행해야 함
     * - 최소 4명의 사용자 (20250001, 20250002, 20250003, 20250004) 필요
     * 
     * 생성 개수: 10개 (현재 월만, 통계용)
     */
    @Test
    public void insertDummyUserBudgets() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
        LocalDate now = LocalDate.now();
        String currentYearMonth = now.format(formatter);

        // 필요한 사용자들
        User user1 = userRepository.findByEmployeeNo("20250001")
                .orElseThrow(() -> new RuntimeException("20250001 사용자를 찾을 수 없습니다. UserRepositoryTests.insertDummyUsers()를 먼저 실행하세요."));
        User user2 = userRepository.findByEmployeeNo("20250002")
                .orElseThrow(() -> new RuntimeException("20250002 사용자를 찾을 수 없습니다. UserRepositoryTests.insertDummyUsers()를 먼저 실행하세요."));
        User user3 = userRepository.findByEmployeeNo("20250003")
                .orElseThrow(() -> new RuntimeException("20250003 사용자를 찾을 수 없습니다. UserRepositoryTests.insertDummyUsers()를 먼저 실행하세요."));
        User user4 = userRepository.findByEmployeeNo("20250004")
                .orElseThrow(() -> new RuntimeException("20250004 사용자를 찾을 수 없습니다. UserRepositoryTests.insertDummyUsers()를 먼저 실행하세요."));

        // 현재 월 예산 생성 (통계용)
        // user1: 200만원 (25% 소진율 예정, 예산 초과 주의 인원 아님)
        createBudget(user1, currentYearMonth, 2000000, null);
        
        // user2: 임시로 큰 값 설정 (나중에 updateBudgetsForOverBudgetUsers에서 조정)
        createBudget(user2, currentYearMonth, 5000000, null);
        
        // user3: 임시로 큰 값 설정 (나중에 updateBudgetsForOverBudgetUsers에서 조정)
        createBudget(user3, currentYearMonth, 5000000, null);
        
        // user4: 정상 예산
        createBudget(user4, currentYearMonth, 2000000, null);

        log.info("사용자별 월간 예산 더미 데이터 생성 완료 (현재 월 10개)");
    }

    /**
     * 예산 초과 주의 인원 생성 (80-90% 소진율로 예산 조정)
     * ExpenseRepositoryTests.insertDummyExpenses() 실행 후 사용
     * 
     * ⚠️ 주의: user1은 제외 (페이징 테스트용, 예산 초과 주의 인원 아님)
     */
    @Test
    public void updateBudgetsForOverBudgetUsers() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
        String currentYearMonth = LocalDate.now().format(formatter);

        // user1은 제외 (예산 초과 주의 인원 아님)
        User user2 = userRepository.findByEmployeeNo("20250002")
                .orElseThrow(() -> new RuntimeException("20250002 사용자를 찾을 수 없습니다."));
        User user3 = userRepository.findByEmployeeNo("20250003")
                .orElseThrow(() -> new RuntimeException("20250003 사용자를 찾을 수 없습니다."));

        // user2: 목표 85% 소진율 (예산 초과 주의)
        updateBudgetForUser(user2, currentYearMonth, 0.85);

        // user3: 목표 90% 소진율 (예산 초과 주의)
        updateBudgetForUser(user3, currentYearMonth, 0.90);

        log.info("예산 초과 주의 인원 예산 업데이트 완료 (user2, user3)");
    }

    private void updateBudgetForUser(User user, String yearMonth, double targetUsageRate) {
        // 현재 월의 APPROVED 지출 금액 합계 계산
        // ✅ 최적화: findAll() 대신 적절한 쿼리 사용 (receiptDate 기준)
        LocalDate startDate = LocalDate.parse(yearMonth + "-01");
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
        
        List<Expense> approvedExpenses = expenseRepository.findForReportByUserIdAndDateRange(
                user.getId(),
                startDate,
                endDate
        );

        int totalExpense = approvedExpenses.stream()
                .mapToInt(Expense::getAmount)
                .sum();

        if (totalExpense == 0) {
            log.warn("⚠️ User {}의 현재 월 APPROVED 지출이 없습니다. 예산 조정을 건너뜁니다.", user.getEmployeeNo());
            return;
        }

        // 목표 소진율에 맞춰 예산 계산
        // 예: 지출 170만원, 목표 85% 소진율 → 예산 = 170만원 / 0.85 = 200만원
        int calculatedBudget = Math.max((int)(totalExpense / targetUsageRate), 100000); // 최소 10만원

        // 예산 업데이트 또는 생성
        userBudgetMonthlyRepository.findByUserIdAndYearMonth(user.getId(), yearMonth)
                .ifPresentOrElse(
                    budget -> {
                        budget.setMonthlyLimit(calculatedBudget);
                        userBudgetMonthlyRepository.save(budget);
                        double actualRate = (totalExpense * 100.0 / calculatedBudget);
                        log.info("예산 업데이트: User={}, YearMonth={}, Limit={}원, 지출={}원, 소진율={:.2f}%", 
                                user.getEmployeeNo(), yearMonth, calculatedBudget, totalExpense, actualRate);
                    },
                    () -> {
                        UserBudgetMonthly budget = UserBudgetMonthly.builder()
                                .user(user)
                                .yearMonth(yearMonth)
                                .monthlyLimit(calculatedBudget)
                                .note(null)
                                .build();
                        userBudgetMonthlyRepository.save(budget);
                        double actualRate = (totalExpense * 100.0 / calculatedBudget);
                        log.info("예산 생성: User={}, YearMonth={}, Limit={}원, 지출={}원, 소진율={:.2f}%", 
                                user.getEmployeeNo(), yearMonth, calculatedBudget, totalExpense, actualRate);
                    }
                );
    }

    private void createBudget(User user, String yearMonth, int monthlyLimit, String note) {
        // 이미 존재하는지 확인
        if (userBudgetMonthlyRepository.findByUserIdAndYearMonth(user.getId(), yearMonth).isEmpty()) {
            UserBudgetMonthly budget = UserBudgetMonthly.builder()
                    .user(user)
                    .yearMonth(yearMonth)
                    .monthlyLimit(monthlyLimit)
                    .note(note)
                    .build();
            userBudgetMonthlyRepository.save(budget);
            log.info("예산 생성: User={}, YearMonth={}, Limit={}", user.getEmployeeNo(), yearMonth, monthlyLimit);
        }
    }
}

