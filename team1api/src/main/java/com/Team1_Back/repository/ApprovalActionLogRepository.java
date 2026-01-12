package com.Team1_Back.repository;

import com.Team1_Back.domain.ApprovalActionLog;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ApprovalActionLogRepository extends JpaRepository<ApprovalActionLog, Long> {

    // 결재 요청 ID로 액션 로그 조회 (타임라인) - approvalRequest, actor 함께 로드
    @EntityGraph(attributePaths = {"approvalRequest", "actor"})
    @Query("SELECT aal FROM ApprovalActionLog aal WHERE aal.approvalRequest.id = :approvalRequestId ORDER BY aal.createdAt ASC")
    List<ApprovalActionLog> findByApprovalRequestIdOrderByCreatedAtAsc(@Param("approvalRequestId") Long approvalRequestId);
}

