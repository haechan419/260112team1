package com.Team1_Back.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "approval_action_log", indexes = {
    @Index(name = "idx_approval_request_created", columnList = "approval_request_id, created_at")
})
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"approvalRequest", "actor"})
public class ApprovalActionLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approval_request_id", nullable = false)
    private ApprovalRequest approvalRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id", nullable = false)
    private User actor;

    @Column(name = "action", length = 30, nullable = false)
    private String action; // SUBMIT, APPROVE, REJECT, REQUEST_MORE_INFO, COMMENT

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;
}

