package com.peng.ding.config;

import com.peng.ding.util.RedisReceiver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

@Configuration
public class RedisConfig {

    @Bean
    RedisMessageListenerContainer container (RedisConnectionFactory connectionFactory, MessageListenerAdapter listenerAdapter){
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(listenerAdapter,new PatternTopic("chat"));  //监听名为“chat”的频道
        return container;
    }

    //调用消息，一旦有消息来的时候回调用receiveMessage来进行处理
    @Bean
    MessageListenerAdapter listenerAdapter(RedisReceiver receiver){
        return new MessageListenerAdapter(receiver,"receiveMessage");
    }

    @Bean(name = "stringRedisTemplate")
    StringRedisTemplate template(RedisConnectionFactory connectionFactory){
        return new StringRedisTemplate(connectionFactory);
    }


}
