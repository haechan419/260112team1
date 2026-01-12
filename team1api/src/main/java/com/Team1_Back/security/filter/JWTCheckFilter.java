package com.Team1_Back.security.filter;

import com.Team1_Back.dto.UserDTO;
import com.Team1_Back.util.JWTUtil;
import com.google.gson.Gson;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class JWTCheckFilter extends OncePerRequestFilter {

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {

        // ✅ CORS preflight는 통과
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;

        String path = request.getRequestURI();

        // ✅ 로그인/회원가입 등은 JWT 체크 제외
        if (path.startsWith("/api/auth/")) return true;

        // ✅ 이미지 조회는 JWT 체크 제외 (공개 API)
        if (path.startsWith("/api/view/")) return true;

        return false; // 그 외 /api/**는 필터 적용
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        log.info("[JWTCHK] {} {} auth={}",
                request.getMethod(),
                request.getRequestURI(),
                request.getHeader("Authorization"));



        // ✅ 1) Preflight는 무조건 패스
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();

        // ✅ 2) 인증 없이 열어둔 엔드포인트는 패스
        if (path.startsWith("/api/auth/")) {
            filterChain.doFilter(request, response);
            return;
        }
        String authHeader = request.getHeader("Authorization");

        log.debug("[JWT] path={}, authHeaderPresent={}", path, authHeader != null);

        // ✅ 토큰이 없거나 Bearer 형식이 아니면 "그냥 통과"
        // (최종 차단은 SecurityConfig의 authorizeHttpRequests가 함)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String accessToken = authHeader.substring(7);

        try {
            Map<String, Object> claims = JWTUtil.validateToken(accessToken);
            log.debug("[JWT] claims={}", claims);

            Long id = claims.get("id") != null ? ((Number) claims.get("id")).longValue() : null;
            String employeeNo = (String) claims.get("employeeNo");
            String name = (String) claims.get("name");
            String email = (String) claims.get("email");
            String departmentName = (String) claims.get("departmentName");

            @SuppressWarnings("unchecked")
            List<String> roleNames = (List<String>) claims.get("roleNames");

            // ✅ 여기서 ROLE_ 붙이지 말 것!
            // UserDTO가 authorities 만들 때 ROLE_ 붙이니까 여기서는 ADMIN 형태로 통일
            List<String> pureRoles = (roleNames == null) ? List.of()
                    : roleNames.stream()
                    .map(r -> r.startsWith("ROLE_") ? r.substring("ROLE_".length()) : r) // 혹시 토큰이 ROLE_ADMIN이면 ADMIN으로
                    .collect(Collectors.toList());

            UserDTO userDTO = new UserDTO(
                    id,
                    employeeNo,
                    "",
                    name,
                    email,
                    departmentName,
                    true,
                    false,
                    0,
                    pureRoles
            );

            // ✅ 디버깅용: 지금 authorities가 ROLE_ADMIN인지 확인
            log.debug("[JWT] authorities={}", userDTO.getAuthorities());

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(userDTO, null, userDTO.getAuthorities());

            SecurityContextHolder.getContext().setAuthentication(auth);

            filterChain.doFilter(request, response);

        } catch (Exception e) {
            log.warn("[JWT] invalid token: {}", e.toString());

            Gson gson = new Gson();
            String msg = gson.toJson(Map.of("success", false, "message", "ERROR_ACCESS_TOKEN"));

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json; charset=UTF-8");
            response.getWriter().write(msg);
        }
    }
}
