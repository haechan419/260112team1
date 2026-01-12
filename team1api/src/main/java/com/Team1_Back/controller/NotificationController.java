package com.Team1_Back.controller;

// 👇 [핵심] 이 import 문들이 없어서 빨간 줄이 뜬 겁니다!
import com.Team1_Back.dto.NotificationDTO;
import com.Team1_Back.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notification")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;

    // 1. 내 알림 조회
    @GetMapping("/list")
    public List<NotificationDTO> getMyList(Principal principal) {
        // Principal이 null일 경우(비로그인) 처리
        if (principal == null) {
            log.warn("비로그인 사용자의 알림 요청");
            return List.of();
        }
        
        String mid = principal.getName(); // 로그인한 사용자 ID
        log.info("🔔 알림 조회 요청: " + mid);
        
        return notificationService.getMyNotifications(mid);
    }

    // 2. 읽음 처리
    @PutMapping("/{nno}/read")
    public Map<String, String> read(@PathVariable("nno") Long nno) {
        log.info("👀 알림 읽음 처리: " + nno);
        notificationService.read(nno);
        return Map.of("result", "SUCCESS");
    }
}