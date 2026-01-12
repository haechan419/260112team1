package com.Team1_Back.config;

import com.Team1_Back.service.ChatRoomSecurityService;
import com.Team1_Back.util.CustomJWTException;
import com.Team1_Back.util.JWTUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final ChatRoomSecurityService chatRoomSecurityService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {

        StompHeaderAccessor acc =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (acc == null) return message;

        StompCommand cmd = acc.getCommand();
        if (cmd == null) return message;

        // =========================================================
        // 1) CONNECT: JWT 검증 + Principal(userId) 세팅
        // =========================================================
        if (StompCommand.CONNECT.equals(cmd)) {

            String auth = acc.getFirstNativeHeader("Authorization");
            if (auth == null || !auth.startsWith("Bearer ")) {
                throw new MessagingException("Missing Authorization Bearer token in STOMP CONNECT");
            }

            String token = auth.substring("Bearer ".length()).trim();

            try {
                // ✅ 1) JWT 검증
                Map<String, Object> claims = JWTUtil.validateToken(token);

                // ✅ 2) userId 추출 (id 우선, 없으면 sub도 숫자일 경우 커버)
                Long uid = toLong(claims.get("id"));
                if (uid == null) {
                    uid = toLong(claims.get("sub"));
                }

                if (uid == null) {
                    throw new MessagingException("JWT claims missing numeric user id (id/sub)");
                }

                // ✅ 3) Principal 세팅: 람다 캡쳐는 final/effectively final만 가능
                final String principalName = String.valueOf(uid);
                Principal principal = () -> principalName;
                acc.setUser(principal);

                log.info("[STOMP] CONNECT OK userId={}", principalName);

            } catch (CustomJWTException e) {
                log.warn("[STOMP] CONNECT JWT invalid: {}", e.getMessage());
                throw new MessagingException("Invalid JWT token: " + e.getMessage(), e);
            }
        }

        // =========================================================
        // 2) SUBSCRIBE: /topic/room/{roomId} 구독 권한 체크(멤버만)
        // =========================================================
        if (StompCommand.SUBSCRIBE.equals(cmd)) {
            Long meId = currentUserId(acc);
            String dest = acc.getDestination();

            if (dest != null && dest.startsWith("/topic/room/")) {
                Long roomId = parseRoomId(dest, "/topic/room/");
                if (roomId == null) {
                    throw new MessagingException("Invalid room destination: " + dest);
                }

                boolean ok = chatRoomSecurityService.isMember(meId, roomId);
                if (!ok) {
                    throw new MessagingException("Not allowed to subscribe room=" + roomId);
                }
            }

            // (선택) /user/queue/** 는 기본적으로 user 본인만 받지만,
            // 프론트에서 잘못된 경로 구독을 막고 싶으면 여기서 추가 검증 가능
        }

        // =========================================================
        // 3) SEND: (선택) /app/** 메시지도 권한 체크 가능
        //    - 최종 권한 체크는 Controller/Service에서 한 번 더 하는 걸 권장
        // =========================================================

        return message;
    }

    private Long currentUserId(StompHeaderAccessor acc) {
        if (acc.getUser() == null || acc.getUser().getName() == null) {
            throw new MessagingException("Unauthenticated STOMP session");
        }
        try {
            return Long.valueOf(acc.getUser().getName());
        } catch (Exception e) {
            throw new MessagingException("Invalid principal name (not numeric userId): " + acc.getUser().getName(), e);
        }
    }

    private Long parseRoomId(String dest, String prefix) {
        try {
            return Long.valueOf(dest.substring(prefix.length()));
        } catch (Exception e) {
            return null;
        }
    }

    private Long toLong(Object v) {
        if (v == null) return null;
        if (v instanceof Long l) return l;
        if (v instanceof Integer i) return i.longValue();
        if (v instanceof String s) {
            s = s.trim();
            if (s.isEmpty() || "null".equalsIgnoreCase(s)) return null;
            try {
                return Long.valueOf(s);
            } catch (Exception ignored) {
                return null;
            }
        }
        return null;
    }
}
