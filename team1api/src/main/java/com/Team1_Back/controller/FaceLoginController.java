package com.Team1_Back.controller;

import com.Team1_Back.dto.UserDTO;
import com.Team1_Back.util.JWTUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/face") // 👈 Face ID 전용 경로
@RequiredArgsConstructor
@Slf4j
public class FaceLoginController {

    // 팀원분이 구현해둔 UserDetailsService (CustomUserDetailsService)를 가져옵니다.
    private final UserDetailsService userDetailsService;

    @GetMapping("/login")
    public Map<String, Object> loginByFace(@RequestParam("userId") String employeeNo) {
        
        log.info("Face ID 로그인 요청 - 사원번호: {}", employeeNo);

        // 1. 유저 정보 가져오기 (비밀번호 검사 없이 ID로만 로드)
        // loadUserByUsername은 DB에서 유저 정보를 찾아 UserDTO로 변환해줍니다.
        UserDTO userDTO = (UserDTO) userDetailsService.loadUserByUsername(employeeNo);

        log.info("유저 정보 로드 성공: {}", userDTO.getName());

        // 2. 토큰에 넣을 정보(Claims) 추출
        // (UserDTO에 만들어두신 getClaims() 메서드 활용)
        Map<String, Object> claims = userDTO.getClaims();

        // 3. JWT 토큰 생성 (팀원분의 JWTUtil 사용)
        String accessToken = JWTUtil.generateToken(claims, 10); // 10분
        String refreshToken = JWTUtil.generateToken(claims, 60 * 24); // 24시간

        claims.put("accessToken", accessToken);
        claims.put("refreshToken", refreshToken);

        // 4. 리턴 (진짜 토큰 반환!)
        return claims;
    }
}