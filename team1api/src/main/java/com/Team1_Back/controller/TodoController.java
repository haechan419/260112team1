package com.Team1_Back.controller;

import com.Team1_Back.dto.TodoDTO;
import com.Team1_Back.dto.UserDTO;
import com.Team1_Back.service.TodoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

//Todo 관리를 위한 REST API 컨트롤러
@RestController
@RequestMapping("/api/todos")
@Log4j2
@RequiredArgsConstructor
public class TodoController {

    private final TodoService todoService;

    // Todo를 생성
    @PostMapping
    public ResponseEntity<Map<String, Long>> create(
            @RequestBody TodoDTO todoDTO,
            @AuthenticationPrincipal UserDTO principal) {
        if (principal == null) {
            throw new RuntimeException("인증이 필요합니다.");
        }
        Long userId = principal.getId();
        log.info("Todo 생성 요청: 사용자 ID={}, 제목={}", userId, todoDTO.getTitle());

        Long id = todoService.create(userId, todoDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("id", id));
    }

    // Todo를 조회
    @GetMapping("/{id}")
    public TodoDTO get(
            @PathVariable(name = "id") Long id,
            @AuthenticationPrincipal UserDTO principal) {
        if (principal == null) {
            throw new RuntimeException("인증이 필요합니다.");
        }
        Long userId = principal.getId();
        return todoService.get(id, userId);
    }

    // 사용자의 모든 Todo를 조회
    @GetMapping("/list")
    public List<TodoDTO> getList(@AuthenticationPrincipal UserDTO principal) {
        if (principal == null) {
            throw new RuntimeException("인증이 필요합니다.");
        }
        Long userId = principal.getId();
        return todoService.getList(userId);
    }

    // 사용자의 활성 Todo를 조회 (미완료).
    @GetMapping("/active")
    public List<TodoDTO> getActiveList(@AuthenticationPrincipal UserDTO principal) {
        if (principal == null) {
            throw new RuntimeException("인증이 필요합니다.");
        }
        Long userId = principal.getId();
        return todoService.getActiveList(userId);
    }

    // 사용자의 마감일 지난 미완료 Todo를 조회
    @GetMapping("/overdue")
    public List<TodoDTO> getOverdueList(@AuthenticationPrincipal UserDTO principal) {
        if (principal == null) {
            throw new RuntimeException("인증이 필요합니다.");
        }
        Long userId = principal.getId();
        return todoService.getOverdueList(userId);
    }

    // 날짜 범위로 Todo를 조회
    @GetMapping("/range")
    public List<TodoDTO> getListByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal UserDTO principal) {
        if (principal == null) {
            throw new RuntimeException("인증이 필요합니다.");
        }
        Long userId = principal.getId();
        return todoService.getListByDateRange(userId, startDate, endDate);
    }

    // Todo를 수정
    @PutMapping("/{id}")
    public Map<String, Boolean> update(
            @PathVariable(name = "id") Long id,
            @RequestBody TodoDTO todoDTO,
            @AuthenticationPrincipal UserDTO principal) {
        if (principal == null) {
            throw new RuntimeException("인증이 필요합니다.");
        }
        Long userId = principal.getId();
        log.info("Todo 수정 요청: ID={}, 사용자 ID={}", id, userId);

        todoService.update(id, userId, todoDTO);
        return Map.of("success", true);
    }

    // Todo 상태를 변경
    @PatchMapping("/{id}/status")
    public Map<String, Boolean> updateStatus(
            @PathVariable(name = "id") Long id,
            @RequestParam String status,
            @AuthenticationPrincipal UserDTO principal) {
        if (principal == null) {
            throw new RuntimeException("인증이 필요합니다.");
        }
        Long userId = principal.getId();
        log.info("Todo 상태 변경 요청: ID={}, 상태={}, 사용자 ID={}", id, status, userId);

        todoService.updateStatus(id, userId, status);
        return Map.of("success", true);
    }

    // Todo를 삭제
    @DeleteMapping("/{id}")
    public Map<String, Boolean> delete(
            @PathVariable(name = "id") Long id,
            @AuthenticationPrincipal UserDTO principal) {
        if (principal == null) {
            throw new RuntimeException("인증이 필요합니다.");
        }
        Long userId = principal.getId();
        log.info("Todo 삭제 요청: ID={}, 사용자 ID={}", id, userId);

        todoService.delete(id, userId);
        return Map.of("success", true);
    }
}
