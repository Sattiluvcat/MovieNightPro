package com.satti.service.serviceImpl;

import com.satti.entity.UserBehaviorEvent;
import com.satti.service.RecomConsumeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RecomConsumeServiceImpl implements RecomConsumeService {
    private static final String USER_BEHAVIOR_TOPIC="user-behavior-events";
    private static final Double VIEW_WEIGHT = 2.0;
    private static final Double WATCH_WEIGHT = 5.0;
    private static final Double RATE_WEIGHT = 5.0;

    @Autowired
    private RedisTemplate<String,String> redisTemplate;

    @KafkaListener(topics = USER_BEHAVIOR_TOPIC,groupId = "movie-recommendation-group")
    @Override
    public void consumeUserBehavior(UserBehaviorEvent event){
        updateUserBehavior(event);
        // 生成用户推荐
        generateRecommendations(event.getContact());
    }

    private void updateUserBehavior(UserBehaviorEvent event) {
        String userKey="user_profile:" + event.getContact();
        log.info("Received user behavior event: {}", event);
        // 时间顺序建立用户行为排行榜（不是
        redisTemplate.opsForZSet().add(userKey+":views",event.getMovieId().toString(),event.getTimestamp());
        // 限制排行榜长度为50
        redisTemplate.opsForZSet().removeRange(userKey+":views",0,-51);
        if(event.getBehavior()== UserBehaviorEvent.BehaviorType.RATE){
            redisTemplate.opsForHash().put(userKey+":ratings",event.getMovieId(),event.getRating().toString());
        }
        if(event.getBehavior()== UserBehaviorEvent.BehaviorType.WATCH_TOGETHER){
            redisTemplate.opsForZSet().add(userKey+":watch_together",event.getMovieId().toString(),event.getTimestamp());
        }
    }

    private void generateRecommendations(String contact) {
        Set<String> viewedMovies=redisTemplate.opsForZSet().range("user_profile:"+contact+":views",0,-1);
        Set<String> watchedMovies=redisTemplate.opsForZSet().range("user_profile:"+contact+":watch_together",0,-1);
        // key: movieId, value: rating
        Map<Object,Object> ratedMovies=redisTemplate.opsForHash().entries("user_profile:"+contact+":ratings");
        List<String> recommendations=calculateRecommendations(viewedMovies, watchedMovies,ratedMovies);
        Random random=new Random();
        Collections.shuffle(recommendations,random);
        recommendations=recommendations.subList(0,5);
        redisTemplate.opsForValue().set("recommendations:"+contact,String.join(",",recommendations));
    }

    private List<String> calculateRecommendations(Set<String> viewedMovies, Set<String> watchedMovies,
                                                  Map<Object, Object> ratedMovies) {
        Map<String,Double> userTagPreferences=new HashMap<>();
        setWeightBySet(viewedMovies, userTagPreferences, VIEW_WEIGHT);
        setWeightBySet(watchedMovies, userTagPreferences, WATCH_WEIGHT);
        for(Map.Entry<Object,Object> entry:ratedMovies.entrySet()){
            String movieId=(String)entry.getKey();
            double rating=Double.parseDouble((String)entry.getValue());
            String tagsStr=redisTemplate.opsForValue().get("movie:tags"+movieId);
            if(tagsStr!=null){
                double ratingWeight = rating/5.0 * RATE_WEIGHT;
                for(String tag:tagsStr.split(",")){
                    userTagPreferences.merge(tag,ratingWeight,Double::sum);
                }
            }
        }
        Map<String,Double> candidateMovies=new HashMap<>();
        List<String> topTags=userTagPreferences.entrySet().stream()
                .sorted(Map.Entry.<String,Double>comparingByValue().reversed())
                .limit(50)
                .map(Map.Entry::getKey)
                .toList();
        if(!topTags.isEmpty()){
            List<String> tagKeys = topTags.stream()
                    .map(tag -> "tag:" + tag)
                    .collect(Collectors.toList());

            Set<String> tagBasedMovies;
            if(tagKeys.size() == 1) {
                // 单个标签直接获取
                tagBasedMovies = redisTemplate.opsForSet().members(tagKeys.get(0));
            } else {
                // 多个标签获取并集
                tagBasedMovies = redisTemplate.opsForSet().union(
                        tagKeys.get(0),
                        tagKeys.subList(1, tagKeys.size())
                );
            }
            log.info("Tag-based movie recommendation");
            for(String movieId:tagBasedMovies){
                if(!viewedMovies.contains(movieId)){
                    candidateMovies.merge(movieId,VIEW_WEIGHT*1.0,Double::sum);
                }
            }
        }
        if(candidateMovies.isEmpty()){
            Set<String> popularMovies=redisTemplate.opsForZSet().reverseRange("global:popular",0,10);
            for(String movieId:popularMovies){
                if(!viewedMovies.contains(movieId)){
                    candidateMovies.putIfAbsent(movieId,0.3);
                }
            }
        }
        return candidateMovies.entrySet().stream()
                .sorted(Map.Entry.<String,Double>comparingByValue().reversed())
                .limit(50)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private void setWeightBySet(Set<String> viewedMovies, Map<String, Double> userTagPreferences, Double viewWeight) {
        for(String viewedMovieId:viewedMovies){
            String tagsStr=redisTemplate.opsForValue().get("movie:tags:"+viewedMovieId);
            if(tagsStr!=null){
                for(String tag:tagsStr.split(",")){
                    userTagPreferences.merge(tag, viewWeight,Double::sum);
                }
            }
        }
    }
}
