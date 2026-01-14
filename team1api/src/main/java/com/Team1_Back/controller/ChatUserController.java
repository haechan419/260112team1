package com.Team1_Back.controller;

import com.Team1_Back.dto.UserSearchItemResponse;
import com.Team1_Back.security.CurrentUser;
import com.Team1_Back.service.ChatUserQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatUserController {

    private final ChatUserQueryService chatUserQueryService;

    @GetMapping("/users/search")
    public List<UserSearchItemResponse> searchUsers(
            @RequestParam String q,
            @RequestParam(required = false) Integer limit
    ) {
        Long meId = CurrentUser.id();
        return chatUserQueryService.searchUsers(meId, q, limit);
    }
}

