package com.example.myapp_backend.config;

import com.example.myapp_backend.websocket.NotificationWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.nio.charset.StandardCharsets;

@Configuration
public class ValkeyConfig {

    private static final Logger log = LoggerFactory.getLogger(ValkeyConfig.class);
    private static final String CHANNEL_PREFIX = "notification:";

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            NotificationWebSocketHandler webSocketHandler) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        MessageListener listener = (message, pattern) -> {
            String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
            String body = new String(message.getBody(), StandardCharsets.UTF_8);

            log.debug("Received Valkey message on channel: {}", channel);

            if (channel.startsWith(CHANNEL_PREFIX)) {
                String clientId = channel.substring(CHANNEL_PREFIX.length());
                webSocketHandler.sendNotification(clientId, body);
            }
        };

        container.addMessageListener(listener, new PatternTopic(CHANNEL_PREFIX + "*"));
        return container;
    }
}
