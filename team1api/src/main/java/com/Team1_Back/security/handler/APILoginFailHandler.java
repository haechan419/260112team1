package com.Team1_Back.security.handler;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import com.Team1_Back.security.listener.LoginFailEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import com.google.gson.Gson;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class APILoginFailHandler implements AuthenticationFailureHandler {

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {

        log.info("----------- APILoginFailHandler -----------");

        String employeeNo = request.getParameter("employeeNo");
        String ipAddress = getClientIp(request);

        log.info("로그인 실패 - 사번: {}, IP: {}, 예외: {}, 메시지: {}",
                employeeNo, ipAddress, exception.getClass().getSimpleName(), exception.getMessage());

        // ⭐ 예외 종류에 따라 에러 메시지 결정
        String errorCode;
        String errorMessage;

        if (exception instanceof LockedException) {
            // 계정 잠금
            errorCode = "ACCOUNT_LOCKED";
            errorMessage = "계정이 잠겨있습니다. 관리자에게 문의하세요.";
        } else if (exception instanceof DisabledException) {
            // 계정 비활성화 (퇴사 처리 등)
            errorCode = "ACCOUNT_DISABLED";
            errorMessage = "비활성화된 계정입니다. 관리자에게 문의하세요.";
        } else if (exception instanceof UsernameNotFoundException) {
            // 사용자 없음
            errorCode = "USER_NOT_FOUND";
            errorMessage = "사번 또는 비밀번호가 올바르지 않습니다.";
        } else if (exception instanceof BadCredentialsException) {
            // 비밀번호 불일치
            errorCode = "BAD_CREDENTIALS";
            errorMessage = "사번 또는 비밀번호가 올바르지 않습니다.";

            // 비밀번호 틀린 경우에만 이벤트 발행 (실패 횟수 증가)
            LoginFailEvent event = new LoginFailEvent(employeeNo, ipAddress);
            eventPublisher.publishEvent(event);

            // 이벤트 처리 후 메시지 업데이트 (5회 실패 시 잠금 메시지)
            if (event.getErrorMessage() != null && !event.getErrorMessage().equals("사번 또는 비밀번호가 올바르지 않습니다.")) {
                errorMessage = event.getErrorMessage();
            }
        } else {
            // 기타 인증 실패
            errorCode = "LOGIN_FAILED";
            errorMessage = "로그인에 실패했습니다.";
        }

        log.info("에러 코드: {}, 메시지: {}", errorCode, errorMessage);

        // JSON 응답
        Gson gson = new Gson();
        String jsonStr = gson.toJson(Map.of(
                "success", false,
                "error", errorCode,
                "message", errorMessage
        ));

        response.setContentType("application/json; charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

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
