package com.Team1_Back.controller;

import com.Team1_Back.dto.MeetingNoteDTO;
import com.Team1_Back.dto.UserDTO;
import com.Team1_Back.service.MeetingNoteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

// 회의록 관리를 위한 REST API 컨트롤러
@RestController
@RequestMapping("/api/meeting-notes")
@Log4j2
@RequiredArgsConstructor
public class MeetingNoteController {

    private final MeetingNoteService meetingNoteService;

    // 회의록 파일
    @PostMapping("/upload")
    public Map<String, Long> upload(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDTO principal) {
        if (principal == null) {
            throw new RuntimeException("인증이 필요합니다.");
        }
        Long userId = principal.getId();
        log.info("회의록 업로드 요청: 사용자 ID={}, 파일명={}", userId, file.getOriginalFilename());

        MeetingNoteDTO dto = meetingNoteService.upload(userId, file);
        return Map.of("result", dto.getId());
    }

    // 특정 회의록의 상세 정보를 조회
    @GetMapping("/{id}")
    public MeetingNoteDTO get(
            @PathVariable(name = "id") Long id,
            @AuthenticationPrincipal UserDTO principal) {
        if (principal == null) {
            throw new RuntimeException("인증이 필요합니다.");
        }
        Long userId = principal.getId();
        return meetingNoteService.get(id, userId);
    }

    // 회의록 파일을 다운로드
    @GetMapping("/{id}/file")
    public ResponseEntity<Resource> getFile(
            @PathVariable(name = "id") Long id,
            @AuthenticationPrincipal UserDTO principal) {
        if (principal == null) {
            throw new RuntimeException("인증이 필요합니다.");
        }
        Long userId = principal.getId();

        Resource resource = meetingNoteService.getFile(id, userId);
        MeetingNoteDTO dto = meetingNoteService.get(id, userId);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(dto.getFileType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + dto.getOriginalFileName() + "\"")
                .body(resource);
    }

    // 사용자의 모든 회의록을 조회
    @GetMapping("/list")
    public List<MeetingNoteDTO> getList(@AuthenticationPrincipal UserDTO principal) {
        if (principal == null) {
            throw new RuntimeException("인증이 필요합니다.");
        }
        Long userId = principal.getId();
        return meetingNoteService.getList(userId);
    }

    // 미분석 회의록 목록을 조회
    @GetMapping("/unanalyzed")
    public List<MeetingNoteDTO> getUnanalyzedList(@AuthenticationPrincipal UserDTO principal) {
        if (principal == null) {
            throw new RuntimeException("인증이 필요합니다.");
        }
        Long userId = principal.getId();
        return meetingNoteService.getUnanalyzedList(userId);
    }

    // 회의록을 분석하여 Todo를 자동 생성
    @PostMapping("/{id}/analyze")
    public Map<String, Object> analyze(
            @PathVariable(name = "id") Long id,
            @AuthenticationPrincipal UserDTO principal) {
        if (principal == null) {
            throw new RuntimeException("인증이 필요합니다.");
        }
        Long userId = principal.getId();
        log.info("회의록 분석 요청: ID={}, 사용자 ID={}", id, userId);

        int todoCount = meetingNoteService.analyzeAndCreateTodos(id, userId);
        return Map.of(
                "success", true,
                "todoCount", todoCount);
    }

    // 회의록을 삭제
    @DeleteMapping("/{id}")
    public Map<String, Boolean> remove(
            @PathVariable(name = "id") Long id,
            @AuthenticationPrincipal UserDTO principal) {
        if (principal == null) {
            throw new RuntimeException("인증이 필요합니다.");
        }
        Long userId = principal.getId();
        meetingNoteService.remove(id, userId);
        return Map.of("success", true);
    }
}
