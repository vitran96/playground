package com.example.myapp_backend.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(NotificationWebSocketHandler.class);
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String clientId = UUID.randomUUID().toString();
        session.getAttributes().put("clientId", clientId);
        sessions.put(clientId, session);

        log.info("Client connected. Assigned clientId: {}, session: {}", clientId, session.getId());

        Map<String, String> response = Map.of(
                "type", "CONNECTED",
                "clientId", clientId
        );
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String clientId = (String) session.getAttributes().get("clientId");
        if (clientId != null) {
            sessions.remove(clientId);
            log.info("Client disconnected. clientId: {}, session: {}", clientId, session.getId());
        }
    }

    public void sendNotification(String clientId, String message) {
        WebSocketSession session = sessions.get(clientId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(message));
                log.info("Sent notification to clientId: {}", clientId);
            } catch (IOException e) {
                log.error("Failed to send WebSocket message to clientId: {}", clientId, e);
            }
        }
    }
}
