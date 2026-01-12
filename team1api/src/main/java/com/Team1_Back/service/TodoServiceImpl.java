package com.Team1_Back.service;

import com.Team1_Back.domain.MeetingNote;
import com.Team1_Back.domain.Todo;
import com.Team1_Back.domain.User;
import com.Team1_Back.domain.enums.TodoPriority;
import com.Team1_Back.domain.enums.TodoStatus;
import com.Team1_Back.dto.TodoDTO;
import com.Team1_Back.repository.MeetingNoteRepository;
import com.Team1_Back.repository.TodoRepository;
import com.Team1_Back.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class TodoServiceImpl implements TodoService {

    private final TodoRepository todoRepository;
    private final UserRepository userRepository;
    private final MeetingNoteRepository meetingNoteRepository;
    private final ModelMapper modelMapper;

    @Override
    public Long create(Long userId, TodoDTO todoDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        Todo todo = Todo.builder()
                .user(user)
                .title(todoDTO.getTitle())
                .content(todoDTO.getContent())
                .dueDate(todoDTO.getDueDate())
                .status(todoDTO.getStatus() != null ? TodoStatus.valueOf(todoDTO.getStatus()) : TodoStatus.TODO)
                .priority(todoDTO.getPriority() != null ? TodoPriority.valueOf(todoDTO.getPriority())
                        : TodoPriority.MEDIUM)
                .build();

        // 회의록과 연관된 경우
        if (todoDTO.getMeetingNoteId() != null) {
            MeetingNote meetingNote = meetingNoteRepository.findById(todoDTO.getMeetingNoteId())
                    .orElseThrow(() -> new RuntimeException("회의록을 찾을 수 없습니다."));
            todo.setMeetingNote(meetingNote);
        }

        Todo saved = todoRepository.save(todo);
        log.info("Todo 생성 완료: ID={}, 제목={}", saved.getId(), saved.getTitle());
        return saved.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public TodoDTO get(Long id, Long userId) {
        Todo todo = todoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Todo를 찾을 수 없습니다."));

        // 권한 확인: 본인의 Todo인지 확인
        if (!todo.getUser().getId().equals(userId)) {
            throw new RuntimeException("권한이 없습니다.");
        }

        return entityToDTO(todo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TodoDTO> getList(Long userId) {
        List<Todo> todos = todoRepository.findByUserIdOrderByStatusAscDueDateAsc(userId);
        return todos.stream()
                .map(this::entityToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TodoDTO> getActiveList(Long userId) {
        List<Todo> todos = todoRepository.findActiveTodosByUserId(userId);
        return todos.stream()
                .map(this::entityToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TodoDTO> getOverdueList(Long userId) {
        LocalDate today = LocalDate.now();
        List<Todo> todos = todoRepository.findOverdueTodosByUserId(userId, today);
        return todos.stream()
                .map(this::entityToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TodoDTO> getListByDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        List<Todo> todos = todoRepository.findByUserIdAndDueDateBetween(userId, startDate, endDate);
        return todos.stream()
                .map(this::entityToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public void update(Long id, Long userId, TodoDTO todoDTO) {
        Todo todo = todoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Todo를 찾을 수 없습니다."));

        // 권한 확인
        if (!todo.getUser().getId().equals(userId)) {
            throw new RuntimeException("권한이 없습니다.");
        }

        // 수정
        if (todoDTO.getTitle() != null) {
            todo.setTitle(todoDTO.getTitle());
        }
        if (todoDTO.getContent() != null) {
            todo.setContent(todoDTO.getContent());
        }
        if (todoDTO.getDueDate() != null) {
            todo.setDueDate(todoDTO.getDueDate());
        }
        if (todoDTO.getStatus() != null) {
            todo.setStatus(TodoStatus.valueOf(todoDTO.getStatus()));
        }
        if (todoDTO.getPriority() != null) {
            todo.setPriority(TodoPriority.valueOf(todoDTO.getPriority()));
        }

        todoRepository.save(todo);
        log.info("Todo 수정 완료: ID={}", id);
    }

    @Override
    public void updateStatus(Long id, Long userId, String status) {
        Todo todo = todoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Todo를 찾을 수 없습니다."));

        // 권한 확인
        if (!todo.getUser().getId().equals(userId)) {
            throw new RuntimeException("권한이 없습니다.");
        }

        // 상태 변경
        TodoStatus newStatus = TodoStatus.valueOf(status);
        todo.setStatus(newStatus);

        // 완료 상태로 변경 시 completedAt 설정
        if (newStatus == TodoStatus.DONE) {
            todo.complete();
        } else if (newStatus == TodoStatus.CANCELLED) {
            todo.cancel();
        } else if (newStatus == TodoStatus.IN_PROGRESS) {
            todo.start();
        }

        todoRepository.save(todo);
        log.info("Todo 상태 변경 완료: ID={}, 상태={}", id, status);
    }

    @Override
    public void delete(Long id, Long userId) {
        Todo todo = todoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Todo를 찾을 수 없습니다."));

        // 권한 확인
        if (!todo.getUser().getId().equals(userId)) {
            throw new RuntimeException("권한이 없습니다.");
        }

        todoRepository.delete(todo);
        log.info("Todo 삭제 완료: ID={}", id);
    }

    // Todo 엔티티를 TodoDTO로 변환
    private TodoDTO entityToDTO(Todo entity) {
        TodoDTO dto = modelMapper.map(entity, TodoDTO.class);

        if (entity.getUser() != null) {
            dto.setUserId(entity.getUser().getId());
            dto.setUserName(entity.getUser().getName());
        }

        if (entity.getStatus() != null) {
            dto.setStatus(entity.getStatus().name());
        }

        if (entity.getPriority() != null) {
            dto.setPriority(entity.getPriority().name());
        }

        if (entity.getMeetingNote() != null) {
            dto.setMeetingNoteId(entity.getMeetingNote().getId());
        }

        return dto;
    }
}
