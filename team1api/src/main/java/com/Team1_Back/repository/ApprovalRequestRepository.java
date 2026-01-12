package com.Team1_Back.repository;

import com.Team1_Back.domain.ApprovalRequest;
import com.Team1_Back.domain.ApprovalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, Long> {

    // 요청자별 결재 요청 조회
    Page<ApprovalRequest> findByRequesterId(Long requesterId, Pageable pageable);

    // 요청자 + 상태별 조회
    Page<ApprovalRequest> findByRequesterIdAndStatusSnapshot(Long requesterId, ApprovalStatus statusSnapshot, Pageable pageable);

    // 결재자별 결재 요청 조회
    Page<ApprovalRequest> findByApproverId(Long approverId, Pageable pageable);

    // 요청 타입별 조회
    Page<ApprovalRequest> findByRequestType(String requestType, Pageable pageable);

    // 상태별 조회
    Page<ApprovalRequest> findByStatusSnapshot(ApprovalStatus statusSnapshot, Pageable pageable);

    // 요청 타입 + 상태별 조회
    Page<ApprovalRequest> findByRequestTypeAndStatusSnapshot(
        String requestType, 
        ApprovalStatus statusSnapshot, 
        Pageable pageable
    );

    // 요청 타입 + ref_id로 조회
    @Query("SELECT ar FROM ApprovalRequest ar WHERE ar.requestType = :requestType AND ar.refId = :refId")
    Optional<ApprovalRequest> findByRequestTypeAndRefId(
        @Param("requestType") String requestType,
        @Param("refId") Long refId
    );

    // ID로 조회 (requester, approver 포함)
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"requester", "approver"})
    @Query("SELECT ar FROM ApprovalRequest ar WHERE ar.id = :id")
    Optional<ApprovalRequest> findByIdWithRelations(@Param("id") Long id);

    // 요청자별 결재 요청 조회 (상신일 기준 정렬용)
    @Query(value = "SELECT ar.* FROM approval_request ar " +
           "WHERE ar.requester_id = :requesterId " +
           "ORDER BY ar.created_at DESC, ar.updated_at DESC",
           nativeQuery = true,
           countQuery = "SELECT COUNT(*) FROM approval_request ar WHERE ar.requester_id = :requesterId")
    Page<ApprovalRequest> findByRequesterIdOrderByReceiptDate(@Param("requesterId") Long requesterId, Pageable pageable);

    // 요청자 + 상태별 조회 (상신일 기준 정렬용)
    @Query(value = "SELECT ar.* FROM approval_request ar " +
           "WHERE ar.requester_id = :requesterId AND ar.status_snapshot = :statusSnapshot " +
           "ORDER BY ar.created_at DESC, ar.updated_at DESC",
           nativeQuery = true,
           countQuery = "SELECT COUNT(*) FROM approval_request ar WHERE ar.requester_id = :requesterId AND ar.status_snapshot = :statusSnapshot")
    Page<ApprovalRequest> findByRequesterIdAndStatusSnapshotOrderByReceiptDate(
            @Param("requesterId") Long requesterId, 
            @Param("statusSnapshot") String statusSnapshot, 
            Pageable pageable);

    // 요청 타입별 조회 (상신일 기준 정렬용)
    @Query(value = "SELECT ar.* FROM approval_request ar " +
           "WHERE ar.request_type = :requestType " +
           "ORDER BY ar.created_at DESC, ar.updated_at DESC",
           nativeQuery = true,
           countQuery = "SELECT COUNT(*) FROM approval_request ar WHERE ar.request_type = :requestType")
    Page<ApprovalRequest> findByRequestTypeOrderByReceiptDate(@Param("requestType") String requestType, Pageable pageable);

    // 상태별 조회 (상신일 기준 정렬용)
    @Query(value = "SELECT ar.* FROM approval_request ar " +
           "WHERE ar.status_snapshot = :statusSnapshot " +
           "ORDER BY ar.created_at DESC, ar.updated_at DESC",
           nativeQuery = true,
           countQuery = "SELECT COUNT(*) FROM approval_request ar WHERE ar.status_snapshot = :statusSnapshot")
    Page<ApprovalRequest> findByStatusSnapshotOrderByReceiptDate(@Param("statusSnapshot") String statusSnapshot, Pageable pageable);

    // 요청 타입 + 상태별 조회 (상신일 기준 정렬용)
    @Query(value = "SELECT ar.* FROM approval_request ar " +
           "WHERE ar.request_type = :requestType AND ar.status_snapshot = :statusSnapshot " +
           "ORDER BY ar.created_at DESC, ar.updated_at DESC",
           nativeQuery = true,
           countQuery = "SELECT COUNT(*) FROM approval_request ar WHERE ar.request_type = :requestType AND ar.status_snapshot = :statusSnapshot")
    Page<ApprovalRequest> findByRequestTypeAndStatusSnapshotOrderByReceiptDate(
            @Param("requestType") String requestType, 
            @Param("statusSnapshot") String statusSnapshot, 
            Pageable pageable);

    // 전체 조회 (상신일 기준 정렬용)
    @Query(value = "SELECT ar.* FROM approval_request ar " +
           "ORDER BY ar.created_at DESC, ar.updated_at DESC",
           nativeQuery = true,
           countQuery = "SELECT COUNT(*) FROM approval_request ar")
    Page<ApprovalRequest> findAllOrderByReceiptDate(Pageable pageable);

    // 요청 타입 + 상태 + 상신일 범위 조회
    @Query(value = "SELECT ar.* FROM approval_request ar " +
           "WHERE ar.request_type = :requestType AND ar.status_snapshot = :statusSnapshot " +
           "AND DATE(ar.created_at) BETWEEN :startDate AND :endDate " +
           "ORDER BY ar.created_at DESC, ar.updated_at DESC",
           nativeQuery = true,
           countQuery = "SELECT COUNT(*) FROM approval_request ar WHERE ar.request_type = :requestType AND ar.status_snapshot = :statusSnapshot AND DATE(ar.created_at) BETWEEN :startDate AND :endDate")
    Page<ApprovalRequest> findByRequestTypeAndStatusSnapshotAndDateRange(
            @Param("requestType") String requestType,
            @Param("statusSnapshot") String statusSnapshot,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable);

    // 요청 타입 + 상신일 범위 조회
    @Query(value = "SELECT ar.* FROM approval_request ar " +
           "WHERE ar.request_type = :requestType " +
           "AND DATE(ar.created_at) BETWEEN :startDate AND :endDate " +
           "ORDER BY ar.created_at DESC, ar.updated_at DESC",
           nativeQuery = true,
           countQuery = "SELECT COUNT(*) FROM approval_request ar WHERE ar.request_type = :requestType AND DATE(ar.created_at) BETWEEN :startDate AND :endDate")
    Page<ApprovalRequest> findByRequestTypeAndDateRange(
            @Param("requestType") String requestType,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable);

    // 상태 + 상신일 범위 조회
    @Query(value = "SELECT ar.* FROM approval_request ar " +
           "WHERE ar.status_snapshot = :statusSnapshot " +
           "AND DATE(ar.created_at) BETWEEN :startDate AND :endDate " +
           "ORDER BY ar.created_at DESC, ar.updated_at DESC",
           nativeQuery = true,
           countQuery = "SELECT COUNT(*) FROM approval_request ar WHERE ar.status_snapshot = :statusSnapshot AND DATE(ar.created_at) BETWEEN :startDate AND :endDate")
    Page<ApprovalRequest> findByStatusSnapshotAndDateRange(
            @Param("statusSnapshot") String statusSnapshot,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable);

    // 전체 + 상신일 범위 조회
    @Query(value = "SELECT ar.* FROM approval_request ar " +
           "WHERE DATE(ar.created_at) BETWEEN :startDate AND :endDate " +
           "ORDER BY ar.created_at DESC, ar.updated_at DESC",
           nativeQuery = true,
           countQuery = "SELECT COUNT(*) FROM approval_request ar WHERE DATE(ar.created_at) BETWEEN :startDate AND :endDate")
    Page<ApprovalRequest> findAllByDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable);

    // 전체 + 시작일만 조회
    @Query(value = "SELECT ar.* FROM approval_request ar " +
           "WHERE DATE(ar.created_at) >= :startDate " +
           "ORDER BY ar.created_at DESC, ar.updated_at DESC",
           nativeQuery = true,
           countQuery = "SELECT COUNT(*) FROM approval_request ar WHERE DATE(ar.created_at) >= :startDate")
    Page<ApprovalRequest> findAllByStartDate(
            @Param("startDate") LocalDate startDate,
            Pageable pageable);

    // 전체 + 종료일만 조회
    @Query(value = "SELECT ar.* FROM approval_request ar " +
           "WHERE DATE(ar.created_at) <= :endDate " +
           "ORDER BY ar.created_at DESC, ar.updated_at DESC",
           nativeQuery = true,
           countQuery = "SELECT COUNT(*) FROM approval_request ar WHERE DATE(ar.created_at) <= :endDate")
    Page<ApprovalRequest> findAllByEndDate(
            @Param("endDate") LocalDate endDate,
            Pageable pageable);

    // 요청 타입 + 상태 + 시작일만 조회
    @Query(value = "SELECT ar.* FROM approval_request ar " +
           "WHERE ar.request_type = :requestType AND ar.status_snapshot = :statusSnapshot " +
           "AND DATE(ar.created_at) >= :startDate " +
           "ORDER BY ar.created_at DESC, ar.updated_at DESC",
           nativeQuery = true,
           countQuery = "SELECT COUNT(*) FROM approval_request ar WHERE ar.request_type = :requestType AND ar.status_snapshot = :statusSnapshot AND DATE(ar.created_at) >= :startDate")
    Page<ApprovalRequest> findByRequestTypeAndStatusSnapshotAndStartDate(
            @Param("requestType") String requestType,
            @Param("statusSnapshot") String statusSnapshot,
            @Param("startDate") LocalDate startDate,
            Pageable pageable);

    // 요청 타입 + 상태 + 종료일만 조회
    @Query(value = "SELECT ar.* FROM approval_request ar " +
           "WHERE ar.request_type = :requestType AND ar.status_snapshot = :statusSnapshot " +
           "AND DATE(ar.created_at) <= :endDate " +
           "ORDER BY ar.created_at DESC, ar.updated_at DESC",
           nativeQuery = true,
           countQuery = "SELECT COUNT(*) FROM approval_request ar WHERE ar.request_type = :requestType AND ar.status_snapshot = :statusSnapshot AND DATE(ar.created_at) <= :endDate")
    Page<ApprovalRequest> findByRequestTypeAndStatusSnapshotAndEndDate(
            @Param("requestType") String requestType,
            @Param("statusSnapshot") String statusSnapshot,
            @Param("endDate") LocalDate endDate,
            Pageable pageable);

    // 요청 타입 + 시작일만 조회
    @Query(value = "SELECT ar.* FROM approval_request ar " +
           "WHERE ar.request_type = :requestType " +
           "AND DATE(ar.created_at) >= :startDate " +
           "ORDER BY ar.created_at DESC, ar.updated_at DESC",
           nativeQuery = true,
           countQuery = "SELECT COUNT(*) FROM approval_request ar WHERE ar.request_type = :requestType AND DATE(ar.created_at) >= :startDate")
    Page<ApprovalRequest> findByRequestTypeAndStartDate(
            @Param("requestType") String requestType,
            @Param("startDate") LocalDate startDate,
            Pageable pageable);

    // 요청 타입 + 종료일만 조회
    @Query(value = "SELECT ar.* FROM approval_request ar " +
           "WHERE ar.request_type = :requestType " +
           "AND DATE(ar.created_at) <= :endDate " +
           "ORDER BY ar.created_at DESC, ar.updated_at DESC",
           nativeQuery = true,
           countQuery = "SELECT COUNT(*) FROM approval_request ar WHERE ar.request_type = :requestType AND DATE(ar.created_at) <= :endDate")
    Page<ApprovalRequest> findByRequestTypeAndEndDate(
            @Param("requestType") String requestType,
            @Param("endDate") LocalDate endDate,
            Pageable pageable);

    // 상태 + 시작일만 조회
    @Query(value = "SELECT ar.* FROM approval_request ar " +
           "WHERE ar.status_snapshot = :statusSnapshot " +
           "AND DATE(ar.created_at) >= :startDate " +
           "ORDER BY ar.created_at DESC, ar.updated_at DESC",
           nativeQuery = true,
           countQuery = "SELECT COUNT(*) FROM approval_request ar WHERE ar.status_snapshot = :statusSnapshot AND DATE(ar.created_at) >= :startDate")
    Page<ApprovalRequest> findByStatusSnapshotAndStartDate(
            @Param("statusSnapshot") String statusSnapshot,
            @Param("startDate") LocalDate startDate,
            Pageable pageable);

    // 상태 + 종료일만 조회
    @Query(value = "SELECT ar.* FROM approval_request ar " +
           "WHERE ar.status_snapshot = :statusSnapshot " +
           "AND DATE(ar.created_at) <= :endDate " +
           "ORDER BY ar.created_at DESC, ar.updated_at DESC",
           nativeQuery = true,
           countQuery = "SELECT COUNT(*) FROM approval_request ar WHERE ar.status_snapshot = :statusSnapshot AND DATE(ar.created_at) <= :endDate")
    Page<ApprovalRequest> findByStatusSnapshotAndEndDate(
            @Param("statusSnapshot") String statusSnapshot,
            @Param("endDate") LocalDate endDate,
            Pageable pageable);

    // 오늘의 미결재 건수 조회 (ApprovalRequest 테이블 기준)
    // DATE() 함수와 CURDATE()를 사용하여 날짜만 비교 (시간 무시)
    @Query(value = 
        "SELECT COUNT(*) " +
        "FROM approval_request ar " +
        "WHERE ar.status_snapshot = 'SUBMITTED' " +
        "  AND DATE(ar.created_at) = DATE(NOW())",
        nativeQuery = true)
    Long countTodayPending();

    // 총 미결재 건수 조회 (ApprovalRequest 테이블 기준)
    // 날짜 제한 없이 SUBMITTED 상태의 모든 건수 조회
    @Query(value = 
        "SELECT COUNT(*) " +
        "FROM approval_request ar " +
        "WHERE ar.status_snapshot = 'SUBMITTED'",
        nativeQuery = true)
    Long countTotalPending();

    // 오늘의 신규 결재 건수 조회 (당일 상신된 모든 건수, 상태 무관)
    @Query(value = 
        "SELECT COUNT(*) " +
        "FROM approval_request ar " +
        "WHERE DATE(ar.created_at) = DATE(NOW())",
        nativeQuery = true)
    Long countTodaySubmitted();

    // 오늘의 처리 건수 조회 (당일 승인/반려된 건수)
    @Query(value = 
        "SELECT COUNT(*) " +
        "FROM approval_request ar " +
        "WHERE (ar.status_snapshot = 'APPROVED' OR ar.status_snapshot = 'REJECTED') " +
        "  AND DATE(ar.updated_at) = DATE(NOW())",
        nativeQuery = true)
    Long countTodayProcessed();

    // 결재 상태별 건수 조회 (전체)
    @Query(value = 
        "SELECT ar.status_snapshot, COUNT(*) " +
        "FROM approval_request ar " +
        "GROUP BY ar.status_snapshot",
        nativeQuery = true)
    List<Object[]> countByStatus();

    // 오늘의 승인 건수 조회
    @Query(value = 
        "SELECT COUNT(*) " +
        "FROM approval_request ar " +
        "WHERE ar.status_snapshot = 'APPROVED' " +
        "  AND DATE(ar.updated_at) = DATE(NOW())",
        nativeQuery = true)
    Long countTodayApproved();

    // 오늘의 반려 건수 조회
    @Query(value = 
        "SELECT COUNT(*) " +
        "FROM approval_request ar " +
        "WHERE ar.status_snapshot = 'REJECTED' " +
        "  AND DATE(ar.updated_at) = DATE(NOW())",
        nativeQuery = true)
    Long countTodayRejected();

    // 오늘의 보완요청 건수 조회
    @Query(value = 
        "SELECT COUNT(*) " +
        "FROM approval_request ar " +
        "WHERE ar.status_snapshot = 'REQUEST_MORE_INFO' " +
        "  AND DATE(ar.updated_at) = DATE(NOW())",
        nativeQuery = true)
    Long countTodayRequestMoreInfo();
}

