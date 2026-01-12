package com.Team1_Back.config;

import java.util.List;

import com.Team1_Back.security.filter.JWTCheckFilter;
import com.Team1_Back.security.handler.APILoginFailHandler;
import com.Team1_Back.security.handler.APILoginSuccessHandler;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    private final ApplicationEventPublisher eventPublisher;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JWTCheckFilter jwtCheckFilter() {
        return new JWTCheckFilter();
    }

    @Bean
    @Order(HIGHEST_PRECEDENCE)
    public CorsConfigurationSource corsConfigurationSource() {

        //  1) 기존 정책 (API용) - 너가 원한대로 그대로 유지
        CorsConfiguration apiConfig = new CorsConfiguration();
        apiConfig.setAllowedOrigins(List.of("http://localhost:3000"));
        apiConfig.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        apiConfig.setAllowedHeaders(List.of(
                "Content-Type",
                "Authorization",
                "Cache-Control",
                "X-User-Id",
                "X-Role",
                "X-Dept"));
        apiConfig.setExposedHeaders(List.of("Content-Disposition"));
        apiConfig.setAllowCredentials(false); // ✅ 그대로 유지

        //  2) SockJS(WebSocket) 전용 정책 - 여기만 credentials 허용
        CorsConfiguration wsConfig = new CorsConfiguration();
        wsConfig.setAllowedOrigins(List.of("http://localhost:3000"));
        wsConfig.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));

        // SockJS가 /info 등에서 여러 헤더를 쓸 수 있어서
        // 최소한 Authorization은 포함, 나머지는 필요하면 추가
        wsConfig.setAllowedHeaders(List.of("Content-Type", "Authorization"));
        wsConfig.setAllowCredentials(true); // ✅ 여기만 true

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        //  더 구체적인 /ws-chat/** 를 먼저 등록 (우선 적용)
        source.registerCorsConfiguration("/ws-chat/**", wsConfig);

        //  나머지는 기존 정책 적용
        source.registerCorsConfiguration("/**", apiConfig);

        return source;
    }
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        log.info("--------------------- security config (JWT + API no-redirect) ---------------------");

        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // ✅ 여기 딱 1번만
                .addFilterBefore(jwtCheckFilter(), UsernamePasswordAuthenticationFilter.class)

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/api/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/reports/**").authenticated()
                        .anyRequest().permitAll()
                )

                .exceptionHandling(e -> e
                        .defaultAuthenticationEntryPointFor(
                                (request, response, authException) -> {
                                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                    response.setContentType("application/json; charset=UTF-8");
                                    response.getWriter().write("{\"success\":false,\"message\":\"UNAUTHORIZED\"}");
                                },
                                new AntPathRequestMatcher("/api/**")
                        )
                )

                .formLogin(form -> form
                        .loginProcessingUrl("/api/auth/login")
                        .usernameParameter("employeeNo")
                        .passwordParameter("password")
                        .successHandler(new APILoginSuccessHandler(eventPublisher))
                        .failureHandler(new APILoginFailHandler(eventPublisher))
                )

                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler((request, response, authentication) -> {
                            response.setStatus(200);
                            response.setContentType("application/json; charset=UTF-8");
                            response.getWriter().write("{\"success\":true,\"message\":\"로그아웃 성공\"}");
                        })
                )
                .httpBasic(basic -> basic.disable());

        http.addFilterBefore(jwtCheckFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

}
