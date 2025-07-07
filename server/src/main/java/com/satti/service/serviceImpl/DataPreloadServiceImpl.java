package com.satti.service.serviceImpl;

import com.satti.entity.Movie;
import com.satti.mapper.MovieMapper;
import com.satti.service.DataPreloadService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@Slf4j
public class DataPreloadServiceImpl implements DataPreloadService {
    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private RedisTemplate<String,String> redisTemplate;
    @Autowired
    private MovieMapper movieMapper;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void preloadData(){
        List<Movie> allMovies=mongoTemplate.findAll(Movie.class);
        log.info("preloading {} movies into Redis", allMovies.size());
        for(Movie movie:allMovies){
            String movieId=String.valueOf(movie.get_id());
            String tags=String.join(",",movie.getTags());
            redisTemplate.opsForValue().set("movie:tags:"+movieId,tags);
            for(String tag:movie.getTags()){
                redisTemplate.opsForSet().add("tag:"+tag,movieId);
            }
        }
        List<Integer> popularMovieIds=movieMapper.getMostView(100);
        for(Integer id:popularMovieIds){
            Integer score=movieMapper.getViewTimes(id);
            redisTemplate.opsForZSet().add("global:popular",
                    String.valueOf(id),
                    score);
        }

        preload();
    }

    private void preload(){
        Set<String> keys = redisTemplate.keys("user_profile:*");
        if (keys != null) redisTemplate.delete(keys);

        String sql = """
            SELECT contact, movie_id, behavior_type, timestamp
            FROM (
                SELECT *, ROW_NUMBER() OVER (
                    PARTITION BY contact ORDER BY timestamp DESC
                ) AS rn FROM user_behavior
            ) t WHERE rn <= 50
            """;

        jdbcTemplate.query(sql, rs -> {
            String contact = rs.getString("contact");
            String movieId = rs.getString("movie_id");
            String behavior = rs.getString("behavior_type");
            long timestamp = rs.getLong("timestamp");

            // 重建Redis数据结构
            String userKey = "user_profile:" + contact;
            if ("VIEW".equals(behavior)) {
                redisTemplate.opsForZSet().add(userKey + ":views", movieId, timestamp);
            } else if ("WATCH_TOGETHER".equals(behavior)) {
                redisTemplate.opsForZSet().add(userKey + ":watch_together", movieId, timestamp);
            }
        });
        log.info("从MySQL加载用户行为数据到Redis完成");
    }
}
