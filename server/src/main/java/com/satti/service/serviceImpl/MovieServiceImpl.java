package com.satti.service.serviceImpl;

import com.satti.config.CacheScheduler;
import com.satti.entity.*;
import com.satti.mapper.MovieMapper;
import com.satti.mapper.UserScoreMapper;
import com.satti.service.CacheService;
import com.satti.service.MailService;
import com.satti.service.MovieService;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MovieServiceImpl implements MovieService {

    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private MailService mailService;
    @Autowired
    private MovieMapper movieMapper;
    @Autowired
    private CacheService cacheService;
    @Autowired
    private ElasticsearchOperations esOperations;
    @Autowired
    private RedisTemplate<String,String> redisTemplate;
    @Autowired
    private UserScoreMapper userScoreMapper;


    // 得到电影详细信息
    @Override
    public MovieDTO getMovieDetails(Integer id,HttpSession session) {
        // 添加mapper，每次查询一个id，在mysql中更新一次
        movieMapper.addTimes(id);

        UserInfo userInfo = (UserInfo) session.getAttribute("userInfo");
        Double user_score=userScoreMapper.getScoreByContactAndMovieId(userInfo.getContact(), id);
        Double user_even=userScoreMapper.getEvenScoreByMovieId(id);

        Movie movie = mongoTemplate.findById(id, Movie.class);
        MovieDTO movieDTO=new MovieDTO();
        movieDTO.setScore_user(user_score);
        movieDTO.setScore_even(user_even);
        if (movie != null) {
            movieDTO.setMovie(movie);
        }
        // 尝试ES缓存获取其它信息
        MovieMetaDoc movieMetaDoc = cacheService.getCachedMovieMeta(id);
        if (movieMetaDoc != null) {
            System.out.println("缓存命中！简介："+movieMetaDoc.getSummary());
            movieDTO.setCover_url(movieMetaDoc.getCoverUrl());
            movieDTO.setSummary(movieMetaDoc.getSummary());
            movieDTO.setScore_official(movieMetaDoc.getOfficialRating());
        }else{
            // 通过电影主页url抓取信息
            String url= null;
            if (movie != null) {
                url = movie.getLink();
            }
            try {
                // Call the Python script
                Map<String,String> result=CacheScheduler.getInfoFromDouban(url);

                // Split the result into parts
                if(result.equals("Error")) {
                    movieDTO.setScore_official("暂无评分");
                    movieDTO.setCover_url("");
                    movieDTO.setSummary("暂无简介");
                    throw new RuntimeException("调取信息失败");
                }

                // Set the scraped information to the movieDTO
                movieDTO.setScore_official(result.get("rating"));
                movieDTO.setCover_url(result.get("cover"));
                movieDTO.setSummary(result.get("summary"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return movieDTO;
    }

    @Override
    public List<MovieBigRow> searchMovies(String keyword) {
        Query query = new Query();
        // 电影名 导演 演员名同时根据关键词搜索
        query.addCriteria(new Criteria().orOperator(
                Criteria.where("title").regex(keyword, "i"),
                Criteria.where("director").regex(keyword, "i"),
                Criteria.where("actors").regex(keyword, "i")
        ));
        return findMovieRow(query);
    }

    @Override
    public List<MovieBigRow> categorySearch(String[] tags, String[] languages) {
        Query query = new Query();
        if ((tags == null || tags.length == 0) && (languages == null || languages.length == 0)) {
            throw new RuntimeException("分类和语言至少有一个不为空");
        }
        // 处理各种情况
//        System.out.println(tags.length);
        if (languages == null || languages.length == 0) {
            // 通过 all 查询满足多个标签的结果
//            System.out.println("第一个");
            query.addCriteria(Criteria.where("tags").all((Object[]) tags));
        } else if (tags == null || tags.length == 0) {
//            System.out.println("languages: " + Arrays.toString(languages));
            query.addCriteria(Criteria.where("languages").all((Object[]) languages));
        } else {
//            System.out.println("第三个 tags: " + Arrays.toString(tags));
            query.addCriteria(new Criteria().andOperator(
                    Criteria.where("tags").all((Object[]) tags),
                    Criteria.where("languages").all((Object[]) languages)
            ));
        }
        return findMovieRow(query);
    }

    @Override
    public String getTitle(Integer id) {
        Movie movie = mongoTemplate.findById(id, Movie.class);
        if (movie == null) {
            throw new RuntimeException("电影不存在");
        }
        return movie.getTitle();
    }

    @Override
    public List<MovieBigRow> getSuggestedMovies() {
        Integer[] ids= MovieBigRow.SUGGEST;
        List<MovieBigRow> movieBigRows = new ArrayList<>();
        for(Integer id:ids){
            Movie movie = mongoTemplate.findById(id, Movie.class);
            MovieBigRow movieBigRow = new MovieBigRow();
            if (!Arrays.asList("2", "4", "6", "7","8", "10").contains(movie.getStar_mine())) {
                movie.setStar_mine("暂无评分");
            }
            BeanUtils.copyProperties(movie, movieBigRow);
            movieBigRows.add(movieBigRow);
        }
        return movieBigRows;
    }

    @Override
    public List<MovieBigRow> getAllMovies() {
        List<MovieBigRow> movieBigRows = new ArrayList<>();
        List<Movie> movies = mongoTemplate.findAll(Movie.class);
        System.out.println(movies.size());
        return getMovieRows(movieBigRows, movies);
    }

    @Override
    public boolean watchTogetherWithTitle(Integer id, HttpSession session) {
        UserInfo userInfo = (UserInfo) session.getAttribute("userInfo");
        if (userInfo == null) {
            return false;
        }
        String title = getTitle(id);
        String emailContent = String.format("用户 %s (%s) 邀请你一起看: %s\n联系方式: %s",
                userInfo.getNickname(), userInfo.getTitle(), title, userInfo.getContact());
        System.out.println(emailContent);
        mailService.sendSimpleMail("address@mail.com", "一起看电影", emailContent);
        return true;
    }

    @Override
    public boolean commentWithTitle(Integer id, String comment, HttpSession session) {
        UserInfo userInfo = (UserInfo) session.getAttribute("userInfo");
        if (userInfo == null) {
            return false;
        }
        String title = getTitle(id);
        String info = comment.substring(12, comment.length()-2);
        System.out.println(info);
        String emailContent = String.format("用户 %s (%s) 对电影: %s\n发布了评论: %s\n联系方式: %s",
                userInfo.getNickname(), userInfo.getTitle(), title, info, userInfo.getContact());
        mailService.sendSimpleMail("address@mail.com", "电影评论", emailContent);
        return true;
    }

    @Override
    public CompletableFuture<List<MovieSmallRow>> loadHomeAsync(HttpSession session) {
        Executor asyncExecutor = Executors.newFixedThreadPool(4);

        // 获取当前用户
        UserInfo userInfo = (UserInfo) session.getAttribute("userInfo");

        return CompletableFuture.supplyAsync(() -> {

            // 1. 热度榜
            List<Integer> mostViewRows = movieMapper.getMostView(5);
            List<MovieSmallRow> mostViewMovieRows = getMovieSmallRows(new ArrayList<>(), mostViewRows);
            List<MovieSmallRow> result = new ArrayList<>(mostViewMovieRows);

            // 2. 个性化推荐
            List<MovieSmallRow> personalized = getPersonalizedRecommendations(userInfo);
            result.addAll(personalized);

            return result;
        }, asyncExecutor);
    }

    private List<MovieSmallRow> getPersonalizedRecommendations(UserInfo userInfo) {

        if (userInfo != null) {
            // 从Redis获取推荐结果
            String recs = redisTemplate.opsForValue().get("recommendations:" + userInfo.getContact());

            if (recs != null && !recs.isEmpty()) {
                String[] movieIds = recs.split(",");
                Integer[] ids= Arrays.stream(movieIds)
                        .map(Integer::parseInt)
                        .toArray(Integer[]::new);
                return getMovieSmallRows(new ArrayList<>(), Arrays.asList(ids));
            }
        }
        // 如果没有个性化推荐，返回随机推荐
        return getRandomRecommendations();
    }

    private List<MovieSmallRow> getRandomRecommendations() {
        List<MovieSmallRow> dailyRecommendRows = new ArrayList<>();
        List<Integer> dailyRecommendMovies = new ArrayList<>();

        for(int i = 0; i < 5; i++) {
            dailyRecommendMovies.add(ThreadLocalRandom.current().nextInt(1, 632));
        }

        return getMovieSmallRows(dailyRecommendRows, dailyRecommendMovies);
    }

    /**
     * 模糊搜索（简介）
     * @param keyword
     * @return
     */
    @Override
    public List<MovieBigRow> searchDescriptions(String keyword) {
        org.springframework.data.elasticsearch.core.query.Query query= NativeQuery.builder()
                .withQuery(q->q
                        .match(m->m
                                .field("summary")
                                .query(keyword)
                                .fuzziness("AUTO")
                        )
                )
                .withPageable(PageRequest.of(0,100))
                .build();
        SearchHits<MovieMetaDoc> hits;
        try{
            hits=esOperations.search(
                    query,
                    MovieMetaDoc.class,
                    IndexCoordinates.of("movie_metadata")
            );
        }catch (Exception e){
            log.error("Elasticsearch查询失败: {}", e.getMessage());
            return Collections.emptyList();
        }
        return hits.stream().map(hit->{
            Integer movieId=Integer.parseInt(hit.getId());
            MovieMetaDoc movieMetaDoc=hit.getContent();

            MovieBigRow movieBigRow=new MovieBigRow();
            Movie movie=mongoTemplate.findById(movieId, Movie.class);
            movieBigRow.setSummary(movieMetaDoc.getSummary());
            if (movie != null) {
                BeanUtils.copyProperties(movie, movieBigRow);
            }
            return new MovieBigRow(movieBigRow);
        }).collect(Collectors.toList());
    }

    private List<MovieSmallRow> getMovieSmallRows(List<MovieSmallRow> dailyRecommendRows, List<Integer> suggestMovies) {
        for (Integer id : suggestMovies) {
            Movie movie = mongoTemplate.findById(id, Movie.class);
            if (movie != null) {
                MovieSmallRow movieSmallRow = new MovieSmallRow();
                BeanUtils.copyProperties(movie, movieSmallRow);
                dailyRecommendRows.add(movieSmallRow);
            }
        }
        return dailyRecommendRows;
    }

    // 将Movie转换为MovieRow
    private List<MovieBigRow> getMovieRows(List<MovieBigRow> movieBigRows, List<Movie> movies) {
        for(Movie movie:movies){
            MovieBigRow movieBigRow = new MovieBigRow();
            if (!Arrays.asList("2", "4", "6","7", "8", "10").contains(movie.getStar_mine())) {
                movie.setStar_mine("暂无评分");
            }
            BeanUtils.copyProperties(movie, movieBigRow);
            movieBigRows.add(movieBigRow);
        }
        return movieBigRows;
    }

    // 根据条件在MongoDB库中查询Movie结果并转换为MovieRow
    private List<MovieBigRow> findMovieRow(Query query) {
        List<Movie> movies = mongoTemplate.find(query, Movie.class);
        List<MovieBigRow> movieBigRows = new ArrayList<>();
        return getMovieRows(movieBigRows, movies);
    }
}
