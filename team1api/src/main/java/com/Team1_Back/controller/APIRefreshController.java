package com.Team1_Back.controller;

import com.Team1_Back.util.CustomJWTException;
import com.Team1_Back.util.JWTUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * JWT 토큰 갱신 컨트롤러
 *
 * Access Token이 만료되었을 때 Refresh Token으로 새 토큰 발급
 *
 * 요청 예시:
 * GET /api/auth/refresh?refreshToken=xxx
 * Header: Authorization: Bearer [만료된 accessToken]
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class APIRefreshController {

    /**
     * 토큰 갱신
     *
     * @param authHeader - Authorization 헤더 (Bearer accessToken)
     * @param refreshToken - 쿼리 파라미터로 전달된 Refresh Token
     * @return 새로운 Access Token (+ 필요시 새 Refresh Token)
     */
    @RequestMapping("/api/auth/refresh")
    public Map<String, Object> refresh(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam("refreshToken") String refreshToken) {

        log.info("토큰 갱신 요청");

        // 1. 파라미터 검증
        if (refreshToken == null || refreshToken.isEmpty()) {
            throw new CustomJWTException("NULL_REFRESH");
        }

        if (authHeader == null || authHeader.length() < 7) {
            throw new CustomJWTException("INVALID_STRING");
        }

        // 2. Access Token 추출
        String accessToken = authHeader.substring(7);

        // 3. Access Token이 아직 만료되지 않았으면 그대로 반환
        if (!checkExpiredToken(accessToken)) {
            log.info("Access Token이 아직 유효함 - 그대로 반환");
            return Map.of("accessToken", accessToken, "refreshToken", refreshToken);
        }

        // 4. Refresh Token 검증
        Map<String, Object> claims = JWTUtil.validateToken(refreshToken);
        log.info("Refresh Token 검증 완료 - claims: {}", claims);

        // 5. 새 Access Token 발급 (10분)
        String newAccessToken = JWTUtil.generateToken(claims, 10);

        // 6. Refresh Token 만료 1시간 전이면 새로 발급
        String newRefreshToken = checkTime((Integer) claims.get("exp"))
                ? JWTUtil.generateToken(claims, 60 * 24)  // 새 Refresh Token (24시간)
                : refreshToken;                            // 기존 것 유지

        log.info("새 토큰 발급 완료");

        return Map.of("accessToken", newAccessToken, "refreshToken", newRefreshToken);
    }

    /**
     * Refresh Token 만료까지 1시간 미만 남았는지 확인
     *
     * @param exp - 만료 시간 (Unix timestamp, 초 단위)
     * @return true면 1시간 미만 남음 → 새로 발급 필요
     */
    private boolean checkTime(Integer exp) {
        // JWT exp를 밀리초 단위 Date로 변환
        java.util.Date expDate = new java.util.Date((long) exp * 1000);

        // 현재 시간과의 차이 (밀리초)
        long gap = expDate.getTime() - System.currentTimeMillis();

        // 분 단위로 변환
        long leftMin = gap / (1000 * 60);

        // 1시간(60분) 미만이면 true
        return leftMin < 60;
    }

    /**
     * Access Token 만료 여부 확인
     *
     * @param token - 확인할 토큰
     * @return true면 만료됨
     */
    private boolean checkExpiredToken(String token) {
        try {
            JWTUtil.validateToken(token);
        } catch (CustomJWTException ex) {
            if (ex.getMessage().equals("Expired")) {
                return true;  // 만료됨
            }
        }
        return false;  // 만료되지 않음
    }
}
