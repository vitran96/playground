package com.example.myapp_backend;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/messages")
public class MessageController {

    private static final Logger log = LoggerFactory.getLogger(MessageController.class);

    private final StringRedisTemplate redisTemplate;
    private final ChannelTopic topic;

    public MessageController(StringRedisTemplate redisTemplate, ChannelTopic topic) {
        log.info("MessageController initialized");
        this.redisTemplate = redisTemplate;
        this.topic = topic;
    }

    @PostMapping("/publish")
    public Map<String, String> publish(@RequestBody Map<String, String> body) {
        String message = body.getOrDefault("message", "");
        log.info("POST /api/v1/messages/publish - publishing to channel '{}': {}", topic.getTopic(), message);
        redisTemplate.convertAndSend(topic.getTopic(), message);
        return Map.of("status", "published", "channel", topic.getTopic(), "message", message);
    }
}
