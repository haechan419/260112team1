package com.Team1_Back.config;

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
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;

import java.util.List;

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

        // ===== 1) API용 CORS =====
        CorsConfiguration api = new CorsConfiguration();
        api.setAllowedOrigins(List.of("http://localhost:3000"));
        api.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        api.setAllowedHeaders(List.of(
                "Content-Type",
                "Authorization",
                "Cache-Control",
                "X-User-Id",
                "X-Role",
                "X-Dept"
        ));
        api.setExposedHeaders(List.of("Content-Disposition"));
        api.setAllowCredentials(false); // ✅ JWT 헤더 방식이면 false가 안전

        // ===== 2) WS/SockJS용 CORS =====
        // ⚠️ WS도 JWT 헤더 방식이면 credentials=false가 안전
        CorsConfiguration ws = new CorsConfiguration();
        ws.setAllowedOrigins(List.of("http://localhost:3000"));
        ws.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
        ws.setAllowedHeaders(List.of("Content-Type", "Authorization"));
        ws.setExposedHeaders(List.of("Content-Disposition"));
        ws.setAllowCredentials(true); // ✅ true로 안 켬

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        // ✅ ws가 더 구체적이니까 먼저
        source.registerCorsConfiguration("/ws-chat/**", ws);
        // ✅ 나머지
        source.registerCorsConfiguration("/**", api);

        return source;
    }


    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // ✅ JWT 필터는 딱 1번만
                .addFilterBefore(jwtCheckFilter(), UsernamePasswordAuthenticationFilter.class)

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // ✅ WS는 "무시(ignoring)"가 아니라 "열어주기(permitAll)"로
                        .requestMatchers("/ws-chat/**").permitAll()

                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/reports/**").authenticated()

                        // 여기서 /api/chat/** 를 authenticated로 바꾸고 싶으면 바꿔도 됨
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

        return http.build();
    }
}
