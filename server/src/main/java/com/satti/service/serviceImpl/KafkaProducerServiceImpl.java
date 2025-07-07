package com.satti.service.serviceImpl;

import com.satti.entity.UserBehaviorEvent;
import com.satti.service.KafkaProducerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaProducerServiceImpl implements KafkaProducerService {
    private static final String USER_BEHAVIOR_TOPIC="user-behavior-events";

    @Autowired
    private KafkaTemplate<String,UserBehaviorEvent> kafkaTemplate;

    public void sendUserBehavior(UserBehaviorEvent event){
        kafkaTemplate.send(USER_BEHAVIOR_TOPIC, event);
    }
}
