package com.Team1_Back.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.Team1_Back.domain.enums.TodoPriority;
import com.Team1_Back.domain.enums.TodoStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "todo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Todo extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 소유자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 제목
    @Column(name = "title", nullable = false)
    private String title;

    // 내용
    @Column(name = "content", nullable = false)
    private String content;

    // 마감일
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    // 상태
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private TodoStatus status = TodoStatus.TODO;

    // 우선순위
    @Column(name = "priority", nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private TodoPriority priority = TodoPriority.MEDIUM;

    // 관련 회의록(AI 분석으로 생성된 경우)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_note_id")
    private MeetingNote meetingNote;

    // 완료 일시
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // Todo를 진행 중 상태로 변경
    public void start() {
        this.status = TodoStatus.IN_PROGRESS;
    }

    // Todo를 완료 상태로 변경
    public void complete() {
        this.status = TodoStatus.DONE;
        this.completedAt = LocalDateTime.now();
    }

    // Todo를 취소 상태로 변경
    public void cancel() {
        this.status = TodoStatus.CANCELLED;
    }

    // 완료 여부 확인
    public boolean isCompleted() {
        return this.status == TodoStatus.DONE;
    }

    // 취소 여부 확인
    public boolean isCancelled() {
        return this.status == TodoStatus.CANCELLED;
    }

    // 마감일 지났는지 여부 확인
    public boolean isOverdue() {
        return this.dueDate.isBefore(LocalDate.now());
    }

}
