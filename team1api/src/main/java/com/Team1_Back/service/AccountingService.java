package com.Team1_Back.service;

import com.Team1_Back.dto.DepartmentStatisticsDTO;

import java.util.List;
import java.util.Map;

/**
 * 회계 통계 비즈니스 로직을 처리하는 서비스 인터페이스
 * 
 * @author Team1
 */
public interface AccountingService {

    /**
     * 부서별 지출 통계를 조회합니다.
     * 
     * @param status 필터링할 승인 상태 (선택)
     * @return 부서별 통계 목록
     */
    List<DepartmentStatisticsDTO> getDepartmentStatistics(String status);

    /**
     * 부서 목록을 조회합니다.
     * 
     * @return 부서명 목록
     */
    List<String> getDepartments();

    /**
     * 카테고리별 지출 통계를 조회합니다.
     * 
     * @param status 필터링할 승인 상태 (선택)
     * @return 카테고리별 통계 목록
     */
    List<Map<String, Object>> getCategoryStatistics(String status);

    /**
     * 전체 통계 요약을 조회합니다.
     * 
     * @return 통계 요약 정보를 포함한 Map
     */
    Map<String, Object> getSummary();

    /**
     * 예산 초과 인원 목록을 조회합니다.
     * 
     * @return 예산 초과 인원 정보 목록
     */
    List<Map<String, Object>> getOverBudgetList();

    /**
     * 모든 통계 정보를 한번에 조회합니다 (최적화용).
     * 
     * @return 모든 통계 정보를 포함한 Map
     */
    Map<String, Object> getAllStatistics();

    // 한해찬 추가
    // 월별 지출 조회
    List<Map<String, Object>> getMonthlyExpenseTrend(String status);
}
