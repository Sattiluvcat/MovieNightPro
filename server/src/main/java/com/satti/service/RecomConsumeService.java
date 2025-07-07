package com.satti.service;

import com.satti.entity.UserBehaviorEvent;
import org.springframework.kafka.annotation.KafkaListener;

public interface RecomConsumeService {

    void consumeUserBehavior(UserBehaviorEvent event);
}
