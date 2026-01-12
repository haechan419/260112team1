package com.Team1_Back.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.Team1_Back.domain.Todo;
import com.Team1_Back.domain.enums.TodoStatus;

import org.springframework.data.repository.query.Param;

@Repository
public interface TodoRepository extends JpaRepository<Todo, Long> {
    // 사용자 ID로 모든 Todo 조회(상태, 마감일 순 정렬)
    List<Todo> findByUserIdOrderByStatusAscDueDateAsc(Long userId);

    // 사용자 ID와 상태로 Todo 조회
    List<Todo> findByUserIdAndStatus(Long userId, TodoStatus status);

    // 사용자 ID와 마감일 범위로 Todo 조회
    List<Todo> findByUserIdAndDueDateBetween(Long userId, LocalDate startDate, LocalDate endDate);

    // 사용자 ID, 상태, 마감일 범위로 Todo 조회
    List<Todo> findByUserIdAndStatusAndDueDateBetween(Long userId, TodoStatus status, LocalDate startDate,
            LocalDate endDate);

    // 사용자 ID와 미완료 Todo 조회
    @Query("SELECT t FROM Todo t WHERE t.user.id = :userId "
            + "AND t.status NOT IN (com.Team1_Back.domain.enums.TodoStatus.DONE, com.Team1_Back.domain.enums.TodoStatus.CANCELLED) "
            + "ORDER BY t.dueDate ASC NULLS LAST, t.priority DESC")
    List<Todo> findActiveTodosByUserId(@Param("userId") Long userId);

    // 회의록 ID로 생성된 Todo 조회
    List<Todo> findByMeetingNoteId(Long meetingNoteId);

    // 사용자 ID와 회의록 ID로 Todo 조회
    List<Todo> findByUserIdAndMeetingNoteId(Long userId, Long meetingNoteId);

    // 사용자 ID로 마감일이 지난 미완료 Todo 조회
    @Query("SELECT t FROM Todo t WHERE t.user.id = :userId "
            + "AND t.status NOT IN (com.Team1_Back.domain.enums.TodoStatus.DONE, com.Team1_Back.domain.enums.TodoStatus.CANCELLED) "
            + "AND t.dueDate < :today "
            + "ORDER BY t.dueDate ASC, t.priority DESC")
    List<Todo> findOverdueTodosByUserId(@Param("userId") Long userId, @Param("today") LocalDate today);

}
