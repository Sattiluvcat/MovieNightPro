package com.satti.service.serviceImpl;

import com.satti.entity.UserBehaviorEvent;
import com.satti.service.RecomConsumeService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RecomConsumeServiceImpl implements RecomConsumeService {
    private static final String USER_BEHAVIOR_TOPIC="user-behavior-events";
    private static final Double VIEW_WEIGHT = 2.0;
    private static final Double WATCH_WEIGHT = 5.0;
    private static final Double RATE_WEIGHT = 5.0;

    // 批量写入的缓冲区
    private static final int BATCH_SIZE = 100;
    private final List<UserBehaviorEvent> buffer = Collections.synchronizedList(new ArrayList<>());
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @Autowired
    private RedisTemplate<String,String> redisTemplate;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private void flushBufferToMySQL() {
        if (buffer.isEmpty()) return;

        List<UserBehaviorEvent> copy;
        synchronized (buffer) {
            System.out.println("Flushing buffer to MySQL, size: " + buffer);
            copy = new ArrayList<>(buffer);
            buffer.clear();
        }
        // 去重逻辑：根据 contact, movieId, behavior 以及时间戳
        Map<String, UserBehaviorEvent> latestEvents = copy.stream()
                .collect(Collectors.toMap(
                        event -> event.getContact() + "|" + event.getMovieId() + "|" + event.getBehavior(),
                        event -> event,
                        (existing, replacement) ->
                                existing.getTimestamp() > replacement.getTimestamp() ? existing : replacement
                ));

        List<UserBehaviorEvent> distinctEvents = new ArrayList<>(latestEvents.values());

        String sql = """
        INSERT INTO user_behavior (contact, movie_id, behavior_type, timestamp)
        VALUES (?,?,?,?)
        ON DUPLICATE KEY UPDATE timestamp = VALUES(timestamp)
        """;

        jdbcTemplate.batchUpdate(sql, distinctEvents, distinctEvents.size(), (ps, event) -> {
            ps.setString(1, event.getContact());
            ps.setInt(2, event.getMovieId());
            ps.setString(3, event.getBehavior().name());
            ps.setLong(4, event.getTimestamp());
        });

        log.info("写入/更新 {} 条行为数据到MySQL (去重后: {})",
                copy.size(), distinctEvents.size());
    }

    /*
     * 定时任务，每5秒钟将缓冲区数据写入MySQL
     */
    @PostConstruct
    public void init(){
        scheduler.scheduleAtFixedRate(this::flushBufferToMySQL,5,15, java.util.concurrent.TimeUnit.SECONDS);
    }

    @PreDestroy
    public void onShutdown() {
        flushBufferToMySQL(); // 关闭时强制刷新
        scheduler.shutdown();
    }

    @KafkaListener(topics = USER_BEHAVIOR_TOPIC,groupId = "movie-recommendation-group")
    @Override
    public void consumeUserBehavior(UserBehaviorEvent event){
        updateUserBehavior(event);
        // 生成用户推荐
        generateRecommendations(event.getContact());
        if(event.getRating()==null) {
            synchronized (buffer) {
                buffer.add(event);
                if (buffer.size() >= BATCH_SIZE) {
                    flushBufferToMySQL();
                }
            }
        }
    }

    private void updateUserBehavior(UserBehaviorEvent event) {
        String userKey="user_profile:" + event.getContact();
        log.info("Received user behavior event: {}", event);
        // 时间顺序建立用户行为排行榜（不是
        redisTemplate.opsForZSet().add(userKey+":views",event.getMovieId().toString(),event.getTimestamp());
        // 限制排行榜长度为50
        redisTemplate.opsForZSet().removeRange(userKey+":views",0,-51);
        if(event.getBehavior()== UserBehaviorEvent.BehaviorType.RATE){
            System.out.println("rating: "+event.getRating());
            redisTemplate.opsForHash().put(userKey+":ratings",event.getMovieId().toString(),event.getRating().toString());
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
