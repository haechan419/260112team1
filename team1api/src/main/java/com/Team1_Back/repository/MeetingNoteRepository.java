package com.Team1_Back.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.Team1_Back.domain.MeetingNote;

import org.springframework.data.repository.query.Param;

@Repository
public interface MeetingNoteRepository extends JpaRepository<MeetingNote, Long> {
    // 사용자 ID로 모든 회의록 조회(업로드일시 내림차순)
    List<MeetingNote> findByUserIdOrderByUploadDateDesc(Long userId);

    // 사용자 ID와 분석 완료 여부로 회의록 조회
    List<MeetingNote> findByUserIdAndAnalyzed(Long userId, boolean analyzed);

    // 사용자 ID로 미분석 회의록 조회
    @Query("SELECT mn FROM MeetingNote mn WHERE mn.user.id = :userId AND mn.analyzed = false ORDER BY mn.uploadDate DESC")
    List<MeetingNote> findUnanalyzedByUserId(@Param("userId") Long userId);

    // 파일명으로 회의록 조회( 중복 체크용)
    Optional<MeetingNote> findByFileName(String fileName);

    // 사용자 ID와 파일명으로 회의록 조회
    Optional<MeetingNote> findByUserIdAndFileName(Long userId, String fileName);
}
