package com.Team1_Back.service;

import com.Team1_Back.dto.MeetingNoteDTO;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

// 회의록 비즈니스 로직을 처리하는 서비스 인터페이스
public interface MeetingNoteService {

    // 회의록 파일 업로드
    MeetingNoteDTO upload(Long userId, MultipartFile file);

    // 특정 회의록 조회
    MeetingNoteDTO get(Long id, Long userId);

    // 회의록 파일 조회
    Resource getFile(Long id, Long userId);

    // 사용자 모든 회의록 조회
    List<MeetingNoteDTO> getList(Long userId);

    // 미분석 회의록 목록 조회
    List<MeetingNoteDTO> getUnanalyzedList(Long userId);

    // 회의록 삭제
    void remove(Long id, Long userId);

    // 회의록을 분석하여 Todo를 자동 생성
    int analyzeAndCreateTodos(Long id, Long userId);
}
