package com.Team1_Back.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompAuthChannelInterceptor stompAuthChannelInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // /topic : room broadcast
        // /queue : user 개인 큐
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-chat")
                .setAllowedOriginPatterns("http://localhost:3000")
                .withSockJS()
                .setSessionCookieNeeded(false); // ✅ 핵심
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // ✅ CONNECT/SUBSCRIBE를 가로채서 JWT/멤버검증
        registration.interceptors(stompAuthChannelInterceptor);
    }
}
