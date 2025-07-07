package com.satti.service.serviceImpl;

import com.satti.entity.Movie;
import com.satti.mapper.MovieMapper;
import com.satti.service.DataPreloadService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class DataPreloadServiceImpl implements DataPreloadService {
    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private RedisTemplate<String,String> redisTemplate;
    @Autowired
    private MovieMapper movieMapper;

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
    }
}
