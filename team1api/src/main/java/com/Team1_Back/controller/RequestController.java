package com.Team1_Back.controller;

import com.Team1_Back.dto.RequestDTO;
import com.Team1_Back.service.RequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/requests")
public class RequestController {

    private final RequestService requestService;

    // 1. 결재 상신 (POST /api/requests/)
    @PostMapping("/")
    public Map<String, Long> register(@RequestBody RequestDTO requestDTO) {
        log.info("📝 [Controller] 구매 요청 도착!: " + requestDTO);
        Long rno = requestService.register(requestDTO);
        return Map.of("result", rno);
    }

    // 2. [관리자용] 전체 목록 조회 (GET /api/requests/list)
    // 관리자 페이지에서 모든 사원의 신청 내역을 볼 때 사용
    @GetMapping("/list")
    public List<RequestDTO> getList() {
        return requestService.getList();
    }

    // 3. [✨추가] 내 요청 목록 조회 (GET /api/requests/my)
    // 회원(로그인한 사람)이 본인의 신청 내역만 볼 때 사용 (알림, 히스토리 페이지용)
    @GetMapping("/my")
    public List<RequestDTO> getMyList(Principal principal) {
        if (principal == null) {
            log.warn("비로그인 사용자의 요청입니다.");
            return List.of();
        }

        String mid = principal.getName(); // 로그인한 사용자 ID (email)
        log.info("🔍 내 요청 목록 조회: " + mid);

        // 서비스에 이 메서드가 없으면 아래 [추가 작업]을 참고해서 만들어주세요!
        return requestService.getListByRequester(mid);
    }

    // 4. 상태 변경 (PUT /api/requests/{rno}/status)
    @PutMapping("/{rno}/status")
    public Map<String, String> modifyStatus(
            @PathVariable("rno") Long rno,
            @RequestBody Map<String, String> body
    ) {
        String status = body.get("status");
        String rejectReason = body.get("rejectReason");
        requestService.modifyStatus(rno, status, rejectReason);
        return Map.of("result", "SUCCESS");
    }
}