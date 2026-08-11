package com.example.myapp_backend.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/notification")
public class NotificationController {

    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);
    private static final String CHANNEL_PREFIX = "notification:";

    private final StringRedisTemplate stringRedisTemplate;

    public NotificationController(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @PostMapping("/{clientId}")
    public ResponseEntity<Map<String, Object>> sendNotification(
            @PathVariable("clientId") String clientId,
            @RequestBody String payload) {

        String channel = CHANNEL_PREFIX + clientId;
        log.info("Publishing notification to channel: {} for clientId: {}", channel, clientId);

        stringRedisTemplate.convertAndSend(channel, payload);

        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "clientId", clientId,
                "channel", channel
        ));
    }
}
