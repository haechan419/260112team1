package com.Team1_Back.repository;

import com.Team1_Back.domain.DepartmentPerformance;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * 부서별 실적 더미데이터 생성 테스트
 * AI 분석용 데이터 (8개 부서 × 12개월 × 2년 = 192건)
 * 
 * 실행 방법: 이 테스트를 실행하면 DB에 더미데이터가 삽입됩니다.
 */
@SpringBootTest
@Slf4j
public class DepartmentPerformanceDataInitTest {

    @Autowired
    private DepartmentPerformanceRepository performanceRepository;

    /**
     * 부서별 실적 더미데이터 생성
     * 주의: @Commit으로 인해 실제 DB에 저장됩니다!
     */
    @Test
    @Transactional
    @Commit
    public void insertPerformanceData() {
        // 부서 목록
        List<String> departments = Arrays.asList(
            "개발1팀", "개발2팀", "영업팀", "마케팅팀", "인사팀", "재무팀", "기획팀", "디자인팀"
        );
        
        // 연도 목록
        List<Integer> years = Arrays.asList(2024, 2025);
        
        // 부서별 기본 매출 (월 평균)
        // 영업팀 > 개발팀 > 마케팅팀 > 기획팀 > 디자인팀 > 인사팀 > 재무팀
        long[] baseSales = {
            400000000L,  // 개발1팀
            350000000L,  // 개발2팀
            700000000L,  // 영업팀 (최고)
            280000000L,  // 마케팅팀
            55000000L,   // 인사팀 (지원부서)
            50000000L,   // 재무팀 (지원부서)
            170000000L,  // 기획팀
            140000000L   // 디자인팀
        };
        
        Random random = new Random(42); // 고정 시드로 재현 가능
        int createdCount = 0;
        int skippedCount = 0;
        
        for (int yearIdx = 0; yearIdx < years.size(); yearIdx++) {
            int year = years.get(yearIdx);
            double yearGrowth = yearIdx == 0 ? 1.0 : 1.15; // 2025년은 15% 성장
            
            for (int deptIdx = 0; deptIdx < departments.size(); deptIdx++) {
                String deptName = departments.get(deptIdx);
                long deptBaseSales = baseSales[deptIdx];
                
                // 부서별 성장률 차등 적용
                double deptGrowthRate = getDeptGrowthRate(deptName);
                
                for (int month = 1; month <= 12; month++) {
                    // 이미 존재하는지 확인
                    final int currentMonth = month;  // 람다에서 사용하기 위해 final 변수로 복사
                    boolean exists = performanceRepository.findByDepartmentNameAndYearOrderByMonth(deptName, year)
                        .stream()
                        .anyMatch(p -> p.getMonth().equals(currentMonth));
                    
                    if (exists) {
                        skippedCount++;
                        continue;
                    }
                    
                    // 월별 계절 변동 (3월, 5월, 10-12월 높음 / 8월 낮음)
                    double seasonalFactor = getSeasonalFactor(month);
                    
                    // 랜덤 변동 (-10% ~ +10%)
                    double randomFactor = 0.9 + (random.nextDouble() * 0.2);
                    
                    // 최종 매출 계산
                    long salesAmount = (long) (deptBaseSales * yearGrowth * deptGrowthRate * seasonalFactor * randomFactor);
                    
                    // 계약 건수 (매출 1억당 약 2-3건)
                    int contractCount = Math.max(1, (int) (salesAmount / 50000000L) + random.nextInt(3));
                    
                    // 프로젝트 수 (계약 건수의 60-80%)
                    int projectCount = Math.max(1, (int) (contractCount * (0.6 + random.nextDouble() * 0.2)));
                    
                    // 목표 달성률 (80% ~ 170%)
                    double targetRate = 80 + (seasonalFactor - 0.7) * 100 + random.nextDouble() * 30;
                    targetRate = Math.min(180, Math.max(75, targetRate));
                    
                    DepartmentPerformance performance = DepartmentPerformance.builder()
                        .departmentName(deptName)
                        .year(year)
                        .month(month)
                        .salesAmount(salesAmount)
                        .contractCount(contractCount)
                        .projectCount(projectCount)
                        .targetAchievementRate(BigDecimal.valueOf(targetRate).setScale(2, java.math.RoundingMode.HALF_UP))
                        .build();
                    
                    performanceRepository.save(performance);
                    createdCount++;
                    
                    log.info("✅ {} {}년 {}월 데이터 생성 - 매출: {}원", 
                        deptName, year, month, String.format("%,d", salesAmount));
                }
            }
        }
        
        log.info("========================================");
        log.info("📊 부서 실적 더미데이터 생성 완료!");
        log.info("   - 생성됨: {}건", createdCount);
        log.info("   - 건너뜀(이미 존재): {}건", skippedCount);
        log.info("========================================");
    }
    
    /**
     * 부서별 성장률 (2025년 기준)
     */
    private double getDeptGrowthRate(String deptName) {
        switch (deptName) {
            case "영업팀": return 1.18;    // 18% 성장 (최고)
            case "마케팅팀": return 1.20;  // 20% 성장
            case "디자인팀": return 1.18;  // 18% 성장
            case "개발1팀": return 1.15;   // 15% 성장
            case "기획팀": return 1.15;    // 15% 성장
            case "개발2팀": return 1.12;   // 12% 성장
            case "재무팀": return 1.10;    // 10% 성장
            case "인사팀": return 1.08;    // 8% 성장
            default: return 1.10;
        }
    }
    
    /**
     * 월별 계절 변동 계수
     */
    private double getSeasonalFactor(int month) {
        switch (month) {
            case 1: return 0.85;   // 연초 - 낮음
            case 2: return 0.90;   
            case 3: return 1.10;   // 1분기 마감 - 높음
            case 4: return 1.00;
            case 5: return 1.15;   // 상반기 피크
            case 6: return 1.05;
            case 7: return 0.95;
            case 8: return 0.80;   // 휴가철 - 최저
            case 9: return 1.00;
            case 10: return 1.15;  // 4분기 시작 - 높음
            case 11: return 1.20;  // 연말 실적 - 높음
            case 12: return 1.30;  // 연말 마감 - 최고
            default: return 1.00;
        }
    }
    
    /**
     * 데이터 확인용 테스트
     */
    @Test
    public void checkPerformanceData() {
        List<String> departments = performanceRepository.findAllDepartmentNames();
        log.info("========================================");
        log.info("📊 등록된 부서 목록: {}", departments);
        
        for (String dept : departments) {
            var data2024 = performanceRepository.findByDepartmentNameAndYearOrderByMonth(dept, 2024);
            var data2025 = performanceRepository.findByDepartmentNameAndYearOrderByMonth(dept, 2025);
            
            long total2024 = data2024.stream().mapToLong(DepartmentPerformance::getSalesAmount).sum();
            long total2025 = data2025.stream().mapToLong(DepartmentPerformance::getSalesAmount).sum();
            
            log.info("{}: 2024년 {}건 ({}억) / 2025년 {}건 ({}억)", 
                dept, 
                data2024.size(), String.format("%.1f", total2024 / 100000000.0),
                data2025.size(), String.format("%.1f", total2025 / 100000000.0));
        }
        log.info("========================================");
    }
}

