package com.Team1_Back.service;

import com.Team1_Back.domain.ApprovalStatus;
import com.Team1_Back.dto.DepartmentStatisticsDTO;
import com.Team1_Back.repository.ApprovalRequestRepository;
import com.Team1_Back.repository.ExpenseRepository;
import com.Team1_Back.repository.UserBudgetMonthlyRepository;
import com.Team1_Back.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountingServiceImpl implements AccountingService {

    private final ExpenseRepository expenseRepository;
    private final ApprovalRequestRepository approvalRequestRepository;
    private final UserRepository userRepository;
    private final UserBudgetMonthlyRepository userBudgetMonthlyRepository;

    @Override
    public List<DepartmentStatisticsDTO> getDepartmentStatistics(String status) {
        // status가 null이면 APPROVED로 기본값 설정
        String statusValue = (status != null && !status.isEmpty())
                ? status
                : ApprovalStatus.APPROVED.name();

        log.info("🔍 부서별 통계 조회 - status: {}", statusValue);

        List<Object[]> results = expenseRepository.findDepartmentStatistics(statusValue);

        List<DepartmentStatisticsDTO> dtoList = results.stream()
                .map(row -> DepartmentStatisticsDTO.builder()
                        .departmentName((String) row[0])
                        .expenseCount(((Number) row[1]).longValue())
                        .totalAmount(((Number) row[2]).longValue())
                        .build())
                .collect(Collectors.toList());

        log.info("✅ 부서별 통계 조회 결과 - 총 {}개 부서", dtoList.size());
        if (dtoList.isEmpty()) {
            log.warn("⚠️ 부서별 통계 데이터가 없습니다. 승인된 지출 내역이 없거나 부서 정보가 없을 수 있습니다.");
        }
        return dtoList;
    }

    @Override
    public List<String> getDepartments() {
        log.info("🔍 부서 목록 조회");
        List<String> departments = userRepository.findDistinctDepartmentNames();
        log.info("✅ 부서 목록 조회 결과 - 총 {}개 부서", departments.size());
        return departments;
    }

    @Override
    // 카테고리별 통계 조회
    public List<Map<String, Object>> getCategoryStatistics(String status) {
        String statusValue = (status != null && !status.isEmpty())
                ? status
                : ApprovalStatus.APPROVED.name();

        log.info("🔍 카테고리별 통계 조회 - status: {}", statusValue);

        List<Object[]> results = expenseRepository.findCategoryStatistics(statusValue);

        List<Map<String, Object>> dtoList = results.stream()
                .map(row -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("name", row[0] != null ? (String) row[0] : "기타");
                    map.put("amount", ((Number) row[2]).longValue());
                    return map;
                })
                .collect(Collectors.toList());

        log.info("✅ 카테고리별 통계 조회 결과 - 총 {}개 카테고리", dtoList.size());
        if (dtoList.isEmpty()) {
            log.warn("⚠️ 카테고리별 통계 데이터가 없습니다. 승인된 지출 내역이 없거나 카테고리 정보가 없을 수 있습니다.");
        }
        return dtoList;
    }

    @Override
    // 전체 통계 요약 조회
    public Map<String, Object> getSummary() {
        log.info("🔍 전체 통계 요약 조회");

        // ApprovalRequest 테이블에서 총 미결재 건수 조회 (실제 플랫폼 관례)
        Long totalPendingCount = approvalRequestRepository.countTotalPending();
        Long monthlyTotalExpense = expenseRepository.sumMonthlyTotalExpense(ApprovalStatus.APPROVED.name());

        // Phase 1: 오늘의 신규 결재 건수 (당일 상신된 모든 건수)
        Long todaySubmittedCount = approvalRequestRepository.countTodaySubmitted();

        // Phase 1: 오늘의 처리 건수 (당일 승인/반려된 건수)
        Long todayProcessedCount = approvalRequestRepository.countTodayProcessed();

        // Phase 1: 오늘의 결재 현황 (당일 처리된 건수만)
        Long todayApprovedCount = approvalRequestRepository.countTodayApproved();
        Long todayRejectedCount = approvalRequestRepository.countTodayRejected();
        Long todayRequestMoreInfoCount = approvalRequestRepository.countTodayRequestMoreInfo();

        // 현재 월의 yearMonth 형식 (YYYY-MM)
        String currentYearMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        List<Object[]> overBudgetUsers = userBudgetMonthlyRepository.findOverBudgetUsers(currentYearMonth);

        // Phase 1: 전월 대비 증감률 계산
        LocalDate now = LocalDate.now();
        LocalDate lastMonth = now.minusMonths(1);
        String lastMonthYearMonth = lastMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"));

        // 전월 총 지출액 조회
        Long lastMonthTotalExpense = expenseRepository.sumMonthlyTotalExpenseByYearMonth(
                lastMonthYearMonth, ApprovalStatus.APPROVED.name());
        lastMonthTotalExpense = lastMonthTotalExpense != null ? lastMonthTotalExpense : 0L;

        // 전월 대비 증감률 계산
        double monthlyExpenseChangeRate = 0.0;
        if (lastMonthTotalExpense > 0) {
            long expenseDiff = (monthlyTotalExpense != null ? monthlyTotalExpense : 0L) - lastMonthTotalExpense;
            monthlyExpenseChangeRate = (expenseDiff / (double) lastMonthTotalExpense) * 100.0;
        } else if (monthlyTotalExpense != null && monthlyTotalExpense > 0) {
            // 전월 데이터가 없고 이번 달 데이터가 있으면 100% 증가
            monthlyExpenseChangeRate = 100.0;
        }

        // 예산 집행률 계산: 전체 사용자의 월간 예산 대비 실제 지출 비율
        double totalBudgetExecutionRate = 0.0;
        try {
            // 현재 월의 전체 예산 합계 (더 효율적인 방법)
            Long totalBudget = userBudgetMonthlyRepository.findAll().stream()
                    .filter(ubm -> currentYearMonth.equals(ubm.getYearMonth()))
                    .mapToLong(ubm -> ubm.getMonthlyLimit())
                    .sum();

            // 현재 월의 전체 지출 합계 (APPROVED 상태만)
            Long totalExpense = monthlyTotalExpense != null ? monthlyTotalExpense : 0L;

            if (totalBudget > 0 && totalBudget > 0) {
                totalBudgetExecutionRate = (totalExpense.doubleValue() / totalBudget.doubleValue()) * 100.0;
            } else if (totalBudget == 0) {
                log.warn("⚠️ 현재 월({})의 예산 데이터가 없습니다.", currentYearMonth);
            }

            log.info("📊 예산 집행률 계산 - 총 예산: {}, 총 지출: {}, 집행률: {}%",
                    totalBudget, totalExpense, String.format("%.2f", totalBudgetExecutionRate));
        } catch (Exception e) {
            log.warn("⚠️ 예산 집행률 계산 실패: {}", e.getMessage(), e);
            totalBudgetExecutionRate = 0.0;
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalBudgetExecutionRate", Math.round(totalBudgetExecutionRate * 100.0) / 100.0); // 소수점 2자리
        summary.put("totalPendingCount", totalPendingCount != null ? totalPendingCount : 0L);
        summary.put("monthlyTotalExpense", monthlyTotalExpense != null ? monthlyTotalExpense : 0L);
        summary.put("overBudgetCount", overBudgetUsers.size());

        // Phase 1: 추가 지표
        summary.put("todaySubmittedCount", todaySubmittedCount != null ? todaySubmittedCount : 0L);
        summary.put("todayProcessedCount", todayProcessedCount != null ? todayProcessedCount : 0L);
        summary.put("monthlyExpenseChangeRate", Math.round(monthlyExpenseChangeRate * 100.0) / 100.0);

        // Phase 1: 오늘의 결재 현황 (당일 처리된 건수만)
        summary.put("todayApprovedCount", todayApprovedCount != null ? todayApprovedCount : 0L);
        summary.put("todayRejectedCount", todayRejectedCount != null ? todayRejectedCount : 0L);
        summary.put("todayRequestMoreInfoCount", todayRequestMoreInfoCount != null ? todayRequestMoreInfoCount : 0L);

        log.info("✅ 전체 통계 요약 조회 결과 - 집행률: {}%, 총 미결재: {}건, 월간 지출: {}원, 예산 초과: {}명, " +
                "오늘 상신: {}건, 오늘 처리: {}건, 전월 대비: {}%",
                String.format("%.2f", totalBudgetExecutionRate),
                summary.get("totalPendingCount"),
                summary.get("monthlyTotalExpense"),
                summary.get("overBudgetCount"),
                summary.get("todaySubmittedCount"),
                summary.get("todayProcessedCount"),
                String.format("%.2f", monthlyExpenseChangeRate));
        return summary;
    }

    @Override
    // 예산 초과 인원 리스트 조회
    public List<Map<String, Object>> getOverBudgetList() {
        log.info("🔍 예산 초과 인원 리스트 조회");

        String currentYearMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        List<Object[]> results = userBudgetMonthlyRepository.findOverBudgetUsers(currentYearMonth);

        List<Map<String, Object>> dtoList = results.stream()
                .map(row -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("name", row[1] != null ? (String) row[1] : "");
                    map.put("department", row[2] != null ? (String) row[2] : "");

                    Long monthlyLimit = ((Number) row[3]).longValue();
                    Long totalExpense = ((Number) row[4]).longValue();
                    Long remaining = ((Number) row[5]).longValue();

                    double executionRate = monthlyLimit > 0
                            ? (totalExpense.doubleValue() / monthlyLimit.doubleValue() * 100)
                            : 0.0;

                    map.put("executionRate", Math.round(executionRate));
                    map.put("remaining", remaining);
                    return map;
                })
                .collect(Collectors.toList());

        log.info("✅ 예산 초과 인원 리스트 조회 결과 - 총 {}명", dtoList.size());
        if (dtoList.isEmpty()) {
            log.info("ℹ️ 예산 초과 주의 인원이 없습니다. 모든 인원이 예산의 80% 미만을 사용 중입니다.");
        }
        return dtoList;
    }

    @Override
    // 모든 통계 정보를 한번에 조회 (최적화용)
    public Map<String, Object> getAllStatistics() {
        log.info("🔍 모든 통계 정보 통합 조회 (최적화)");

        Map<String, Object> result = new HashMap<>();
        result.put("summary", getSummary());
        result.put("department", getDepartmentStatistics("APPROVED"));
        result.put("category", getCategoryStatistics("APPROVED"));
        result.put("overBudget", getOverBudgetList());

        log.info("✅ 모든 통계 정보 통합 조회 완료");
        return result;
    }

    // 한해찬 추가
    @Override
    public List<Map<String, Object>> getMonthlyExpenseTrend(String status) {
        String statusValue = (status != null && !status.isEmpty())
                ? status
                : ApprovalStatus.APPROVED.name();

        log.info("월별 지출 추이 조회 - status: {}", statusValue);

        List<Object[]> results = expenseRepository.findMonthlyExpenseTrend(statusValue);

        List<Map<String, Object>> trendList = results.stream()
                .map(row -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("yearMonth", row[0] != null ? (String) row[0] : "");
                    map.put("amount", ((Number) row[1]).longValue());
                    return map;
                })
                .collect(Collectors.toList());

        log.info("월별 지출 추이 조회 결과 - 총 {}개 월", trendList.size());
        return trendList;
    }
}
