package com.Team1_Back.security.handler;

import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

/**
 * 접근 거부 핸들러
 *
 * 인증은 되었지만 권한이 없는 경우 (예: USER가 ADMIN 전용 API 접근)
 * 403 Forbidden 응답 반환
 */
@Slf4j
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {

        log.error("Access Denied: {}", accessDeniedException.getMessage());

        Gson gson = new Gson();
        String jsonStr = gson.toJson(Map.of("error", "ERROR_ACCESSDENIED"));

        response.setContentType("application/json; charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);  // 403
        PrintWriter printWriter = response.getWriter();
        printWriter.println(jsonStr);
        printWriter.close();
    }
}
