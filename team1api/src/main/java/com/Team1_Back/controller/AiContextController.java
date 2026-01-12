package com.Team1_Back.controller;

import com.Team1_Back.dto.AiChatFileResponse;
import com.Team1_Back.dto.AiContextRequest;
import com.Team1_Back.dto.AiContextResponse;
import com.Team1_Back.service.AiChatFileService;
import com.Team1_Back.service.AiContextService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiContextController {

    private final AiContextService aiContextService;
    private final AiChatFileService aiChatFileService;

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

        try {
            return aiContextService.findContext(roomId, query.trim());

        } catch (AccessDeniedException e) {
            log.warn("[AI-CONTEXT] ACCESS DENIED roomId={} msg={}", roomId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e);

        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);

        } catch (Exception e) {
            log.error("[AI-CONTEXT] FAILED roomId={} query='{}'", roomId, query, e);
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

        } catch (AccessDeniedException e) {
            log.warn("[AI-CONTEXT-G] ACCESS DENIED msg={}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage(), e);

        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);

        } catch (Exception e) {
            log.error("[AI-CONTEXT-G] FAILED query='{}'", query, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "AI context failed", e);
        }
    }

    @PostMapping("/find-chat-files-global")
    public AiChatFileResponse findChatFilesGlobal(@RequestBody(required = false) AiContextRequest req) {

        if (req == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "request body is required");
        }

        String q = req.getQuery();
        if (q == null || q.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "query is required");
        }

        try {
            return aiChatFileService.findChatFilesGlobal(q.trim());

        } catch (AccessDeniedException e) {
            // ✅ 인증/권한 예외를 500으로 뭉개지 않기
            // 서비스에서 "UNAUTHORIZED" 던지면 401로
            String msg = (e.getMessage() == null) ? "UNAUTHORIZED" : e.getMessage();
            HttpStatus status = msg.toUpperCase().contains("UNAUTHORIZED")
                    ? HttpStatus.UNAUTHORIZED
                    : HttpStatus.FORBIDDEN;

            log.warn("[AI-CHAT-FILE] ACCESS DENIED q='{}' msg={}", q, msg);
            throw new ResponseStatusException(status, msg, e);

        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);

        } catch (Exception e) {
            // ✅ 원인 스택 반드시 남겨야 다음에 바로 잡힘
            log.error("[AI-CHAT-FILE] FAILED q='{}'", q, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "AI chat file failed", e);
        }
    }
}
