package com.Team1_Back.controller;


import com.Team1_Back.dto.ChatAttachmentSearchRow;
import com.Team1_Back.dto.UserDTO;
import com.Team1_Back.service.ChatAttachmentSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat/attachments")
public class ChatAttachmentSearchController {

    private final ChatAttachmentSearchService searchService;

    // GET /api/chat/attachments/search?q=리포트&limit=20&offset=0
    @GetMapping("/search")
    public List<ChatAttachmentSearchRow> search(
            @RequestParam("q") String q,
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "offset", required = false) Integer offset,
            @AuthenticationPrincipal UserDTO me
    ) {
        // SecurityConfig에서 authenticated 걸려있다는 전제
        return searchService.searchGlobal(me.getId(), q, limit, offset);
    }
}