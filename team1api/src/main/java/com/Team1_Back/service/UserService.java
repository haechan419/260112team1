package com.Team1_Back.service;

import com.Team1_Back.domain.User;

import java.util.Optional;

/**
 * 사용자 서비스 인터페이스
 * 
 * <p>사용자 조회 및 권한 확인 기능을 제공합니다.
 * 
 * <p>현재 내 코드에서 사용하는 메서드:
 * <ul>
 *   <li>{@link #isAdmin(Long)} - ExpenseController, ApprovalRequestController, AdminReceiptController, AccountingController에서 사용</li>
 *   <li>{@link #findById(Long)} - 사용자 조회에 사용</li>
 *   <li>{@link #findByEmployeeNo(String)} - 사용자 조회에 사용</li>
 * </ul>
 * 
 * @author Team1
 */
public interface UserService {
    
    /**
     * ID로 사용자 조회
     * 
     * @param id 사용자 ID
     * @return 사용자 정보
     */
    User findById(Long id);
    
    /**
     * 사원번호로 사용자 조회
     * 
     * @param employeeNo 사원번호
     * @return 사용자 정보
     */
    Optional<User> findByEmployeeNo(String employeeNo);
    
    /**
     * 관리자 권한 확인
     * 
     * @param userId 사용자 ID
     * @return 관리자 여부
     */
    boolean isAdmin(Long userId);
}

