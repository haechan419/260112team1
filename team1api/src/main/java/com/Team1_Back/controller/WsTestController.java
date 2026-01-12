package com.Team1_Back.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class WsTestController {

    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/ping")
    public void ping(Principal principal) {
        String me = principal.getName();
        messagingTemplate.convertAndSendToUser(me, "/queue/ping", "pong");
    }

    @MessageMapping("/rooms/{roomId}/send") // client -> /app/rooms/{roomId}/send
    public void sendToRoom(@DestinationVariable String roomId, Principal principal) {

        Map<String, Object> payload = Map.of(
                "roomId", roomId,
                "from", principal.getName(),
                "text", "hello room!",
                "ts", LocalDateTime.now().toString()
        );

        // server -> all subscribers
        messagingTemplate.convertAndSend("/topic/rooms/" + roomId, payload);
    }
}