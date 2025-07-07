package com.satti.service.serviceImpl;

import com.satti.entity.UserBehaviorEvent;
import com.satti.entity.UserInfo;
import com.satti.service.KafkaProducerService;
import com.satti.service.UserBehaveService;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class UserBehaveServiceImpl implements UserBehaveService {
    @Autowired
    private KafkaProducerService kafkaProducerService;

    @Override
    public void viewEvent(Integer id, HttpSession session) {
        UserInfo userInfo = (UserInfo) session.getAttribute("userInfo");
        if(userInfo != null) {
            log.info("User {} set viewed movie with ID: {}", userInfo.getNickname(), id);
            UserBehaviorEvent userBehaviorEvent = new UserBehaviorEvent();
            userBehaviorEvent.setBehavior(UserBehaviorEvent.BehaviorType.VIEW);
            userBehaviorEvent.setContact(userInfo.getContact());
            userBehaviorEvent.setMovieId(id);
            userBehaviorEvent.setTimestamp(System.currentTimeMillis());
            kafkaProducerService.sendUserBehavior(userBehaviorEvent);
        }
    }

    @Override
    public void watchTogetherEvent(Integer id, HttpSession session) {
        UserInfo userInfo = (UserInfo) session.getAttribute("userInfo");
        if(userInfo != null) {
            log.info("User {} set watched together movie with ID: {}", userInfo.getNickname(), id);
            UserBehaviorEvent userBehaviorEvent = new UserBehaviorEvent();
            userBehaviorEvent.setBehavior(UserBehaviorEvent.BehaviorType.WATCH_TOGETHER);
            userBehaviorEvent.setContact(userInfo.getContact());
            userBehaviorEvent.setMovieId(id);
            userBehaviorEvent.setTimestamp(System.currentTimeMillis());
            kafkaProducerService.sendUserBehavior(userBehaviorEvent);
        }
    }

    @Override
    public void rateEvent(Integer id, String rate,HttpSession session) {
        UserInfo userInfo = (UserInfo) session.getAttribute("userInfo");
        if(userInfo != null) {
            log.info("User {} set rate movie with ID: {}, with rate: {}", userInfo.getNickname(), id,rate);
            UserBehaviorEvent userBehaviorEvent = new UserBehaviorEvent();
            userBehaviorEvent.setBehavior(UserBehaviorEvent.BehaviorType.RATE);
            userBehaviorEvent.setContact(userInfo.getContact());
            userBehaviorEvent.setMovieId(id);
            if(rate.charAt(12)=='0'){
                userBehaviorEvent.setRating(10.0);
            } else userBehaviorEvent.setRating(Double.parseDouble(String.valueOf(rate.charAt(11))));
            userBehaviorEvent.setTimestamp(System.currentTimeMillis());
            kafkaProducerService.sendUserBehavior(userBehaviorEvent);
        }
    }
}
