package com.Team1_Back.repository;

import com.Team1_Back.domain.ReceiptUpload;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ReceiptUploadRepository extends JpaRepository<ReceiptUpload, Long> {

    // 지출 ID로 영수증 조회
    @EntityGraph(attributePaths = {"expense", "uploadedBy"})
    @Query("SELECT r FROM ReceiptUpload r WHERE r.expense.id = :expenseId")
    Optional<ReceiptUpload> findByExpenseId(@Param("expenseId") Long expenseId);

    // 파일 해시로 중복 확인
    Optional<ReceiptUpload> findByFileHash(String fileHash);

    // 모든 영수증 조회 (관리자용) - expense와 uploadedBy 함께 로드
    @EntityGraph(attributePaths = {"expense", "uploadedBy"})
    @Query("SELECT r FROM ReceiptUpload r")
    Page<ReceiptUpload> findAllWithRelations(Pageable pageable);
}

