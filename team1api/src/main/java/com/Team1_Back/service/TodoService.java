package com.Team1_Back.service;

import com.Team1_Back.dto.TodoDTO;

import java.time.LocalDate;
import java.util.List;

//Todo 비즈니스 로직을 처리하는 서비스 인터페이스
public interface TodoService {

    // Todo 생성
    Long create(Long userId, TodoDTO todoDTO);

    // 특정 Todo 조회
    TodoDTO get(Long id, Long userId);

    // 사용자 모든 Todo 조회
    List<TodoDTO> getList(Long userId);

    // 사용자의 활성 Todo를 조회 (미완료).
    List<TodoDTO> getActiveList(Long userId);

    // 사용자의 마감일 지난 미완료 Todo 조회
    List<TodoDTO> getOverdueList(Long userId);

    // 날짜 범위로 Todo를 조회
    List<TodoDTO> getListByDateRange(Long userId, LocalDate startDate, LocalDate endDate);

    // Todo 수정
    void update(Long id, Long userId, TodoDTO todoDTO);

    // Todo 상태를 변경
    void updateStatus(Long id, Long userId, String status);

    // Todo 삭제
    void delete(Long id, Long userId);
}
