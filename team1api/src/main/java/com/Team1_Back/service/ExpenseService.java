package com.Team1_Back.service;

import com.Team1_Back.dto.ExpenseDTO;
import com.Team1_Back.dto.ExpenseSubmitDTO;
import com.Team1_Back.dto.PageRequestDTO;
import com.Team1_Back.dto.PageResponseDTO;

import java.time.LocalDate;

/**
 * 지출 내역 비즈니스 로직을 처리하는 서비스 인터페이스
 * 
 * @author Team1
 */
public interface ExpenseService {

    /**
     * 사용자의 지출 내역 목록을 조회합니다.
     * 
     * @param userId 사용자 ID
     * @param pageRequestDTO 페이지네이션 정보
     * @param status 필터링할 승인 상태 (선택)
     * @param startDate 조회 시작 날짜 (선택)
     * @param endDate 조회 종료 날짜 (선택)
     * @return 페이지네이션된 지출 내역 목록
     */
    PageResponseDTO<ExpenseDTO> getList(Long userId, PageRequestDTO pageRequestDTO, String status, LocalDate startDate, LocalDate endDate);

    /**
     * 특정 지출 내역을 조회합니다.
     * 
     * @param id 지출 내역 ID
     * @param userId 요청한 사용자 ID
     * @param isAdmin 관리자 여부
     * @return 지출 내역 정보
     * @throws NoSuchElementException 해당 ID의 지출 내역이 없거나 권한이 없는 경우
     */
    ExpenseDTO get(Long id, Long userId, boolean isAdmin);

    /**
     * 새로운 지출 내역을 등록합니다.
     * 
     * @param expenseDTO 등록할 지출 내역 정보
     * @param userId 작성자 ID
     * @return 등록된 지출 내역의 ID
     */
    Long register(ExpenseDTO expenseDTO, Long userId);

    /**
     * 지출 내역을 수정합니다.
     * 
     * @param expenseDTO 수정할 지출 내역 정보
     * @param userId 요청한 사용자 ID
     * @throws IllegalStateException DRAFT 상태가 아니거나 권한이 없는 경우
     */
    void modify(ExpenseDTO expenseDTO, Long userId);

    /**
     * 지출 내역을 삭제합니다.
     * 
     * @param id 삭제할 지출 내역 ID
     * @param userId 요청한 사용자 ID
     * @throws IllegalStateException DRAFT 상태가 아니거나 권한이 없는 경우
     */
    void remove(Long id, Long userId);

    /**
     * 지출 내역을 승인 요청 상태로 제출합니다.
     * 
     * @param id 제출할 지출 내역 ID
     * @param userId 요청한 사용자 ID
     * @param submitDTO 제출 시 추가 정보 (선택)
     * @throws IllegalStateException DRAFT 상태가 아니거나 권한이 없는 경우
     */
    void submit(Long id, Long userId, ExpenseSubmitDTO submitDTO);

    /**
     * 여러 지출 내역 ID로 한번에 조회합니다 (관리자용).
     * 
     * @param ids 조회할 지출 내역 ID 목록
     * @return 지출 내역 정보 목록 (ID를 키로 하는 Map)
     */
    java.util.Map<Long, ExpenseDTO> getByIds(java.util.List<Long> ids);
}

