package com.example.chatapp.config;

import com.example.chatapp.service.ActiveUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.HashMap;
import java.util.Map;

@Component
public class WebSocketEventListener {

    @Autowired private ActiveUserService activeUserService;
    @Autowired private SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        // 使用会话ID查找并删除用户。这是最可靠的方法。
        String username = activeUserService.removeUserBySessionId(sessionId);

        if (username != null) {
            System.out.println("User Disconnected: " + username + " (Session: " + sessionId + ")");

            // 广播用户下线状态
            Map<String, Object> statusUpdate = new HashMap<>();
            statusUpdate.put("username", username);
            statusUpdate.put("status", "OFFLINE");
            messagingTemplate.convertAndSend("/topic/status", statusUpdate);
        }
    }
}
