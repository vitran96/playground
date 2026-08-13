package com.example.myapp_backend;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
public class PubSubConfig {

    private static final Logger log = LoggerFactory.getLogger(PubSubConfig.class);

    @Bean
    public ChannelTopic itemsTopic() {
        log.info("PubSubConfig - registering channel topic 'items-channel'");
        return new ChannelTopic("items-channel");
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory, ChannelTopic itemsTopic) {
        log.info("PubSubConfig - configuring RedisMessageListenerContainer");
        var container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(
                (message, pattern) -> log.info("Subscriber received: {}", new String(message.getBody())),
                itemsTopic);
        return container;
    }
}
