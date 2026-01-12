package com.Team1_Back.security.handler;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import com.Team1_Back.dto.UserDTO;
import com.Team1_Back.security.listener.LoginSuccessEvent;
import com.Team1_Back.util.JWTUtil;
import com.google.gson.Gson;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class APILoginSuccessHandler implements AuthenticationSuccessHandler {

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        log.info("----------- APILoginSuccessHandler -----------");

        UserDTO userDTO = (UserDTO) authentication.getPrincipal();
        String ipAddress = getClientIp(request);

        log.info("로그인 성공 - 사번: {}, IP: {}", userDTO.getEmployeeNo(), ipAddress);

        // 이벤트 발행 (실패 횟수 초기화, 로그인 기록 저장)
        eventPublisher.publishEvent(new LoginSuccessEvent(userDTO.getEmployeeNo(), ipAddress));

        // ═══════════════════════════════════════════════════════════════
        // JWT 토큰 생성
        // ═══════════════════════════════════════════════════════════════
        Map<String, Object> claims = userDTO.getClaims();

        // Access Token: 60분 유효
        String accessToken = JWTUtil.generateToken(claims, 60*3);

        // Refresh Token: 24시간 유효
        String refreshToken = JWTUtil.generateToken(claims, 60 * 24);

        // 응답에 토큰 추가
        claims.put("accessToken", accessToken);
        claims.put("refreshToken", refreshToken);
        claims.put("success", true);
        claims.put("message", "로그인 성공");

        log.info("JWT 발급 완료 - Access Token 유효시간: 60분, Refresh Token 유효시간: 24시간");

        // JSON 응답
        Gson gson = new Gson();
        String jsonStr = gson.toJson(claims);

        response.setContentType("application/json; charset=UTF-8");
        PrintWriter printWriter = response.getWriter();
        printWriter.println(jsonStr);
        printWriter.close();
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
