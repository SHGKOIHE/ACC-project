package com.foodgroup.chat.config;

import com.foodgroup.chat.pubsub.RedisChatPublisher;
import com.foodgroup.chat.pubsub.RedisChatSubscriber;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
public class RedisPubSubConfig {

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            RedisChatSubscriber redisChatSubscriber
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(redisChatSubscriber, new PatternTopic(RedisChatPublisher.ROOM_CHANNEL_PREFIX + "*"));
        return container;
    }
}
