package com.satti.service.serviceImpl;

import com.satti.service.PostService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PostServiceImpl implements PostService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Bean
    public ApplicationListener<ContextClosedEvent> shutdownListener(){
        return event -> {
            redisTemplate.delete(redisTemplate.keys("user_profile:*"));
            log.info("已清空Redis用户行为数据");
        };
    }
}
