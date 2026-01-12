package com.Team1_Back.controller;

import com.Team1_Back.dto.AiContextRequest;
import com.Team1_Back.dto.AiContextResponse;
import com.Team1_Back.service.AiContextService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiContextController {

    private final AiContextService aiContextService;

    @PostMapping("/find-context")
    public AiContextResponse findContext(@RequestBody(required = false) AiContextRequest request) {

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "request body is required");
        }

        Long roomId = request.getRoomId();
        String query = request.getQuery();

        if (roomId == null || roomId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "roomId is required");
        }

        if (query == null || query.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "query is required");
        }

        // ✅ (선택) 여기에 권한 체크 붙이는 자리:
        // - 현재 로그인 유저가 roomId 멤버인지 검사
        // - 아니면 403
        // authService.assertRoomMember(roomId);

        try {
            return aiContextService.findContext(roomId, query.trim());
        } catch (IllegalArgumentException e) {
            // 서비스에서 검증 에러 던질 경우
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (Exception e) {
            // LLM API 실패/파싱 실패 등
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "AI context failed", e);
        }
    }

    @PostMapping("/find-context-global")
    public AiContextResponse findContextGlobal(@RequestBody(required = false) AiContextRequest request) {

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "request body is required");
        }

        String query = request.getQuery();

        if (query == null || query.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "query is required");
        }

        try {
            return aiContextService.findContextGlobal(query.trim());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "AI context failed", e);
        }
    }

}
