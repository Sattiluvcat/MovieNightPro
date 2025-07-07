package com.satti.service;

import com.satti.entity.UserBehaviorEvent;

public interface KafkaProducerService {
    void sendUserBehavior(UserBehaviorEvent event);
}
