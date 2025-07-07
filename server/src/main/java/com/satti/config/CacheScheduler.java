package com.satti.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.satti.entity.Movie;
import com.satti.service.CacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;

@Slf4j
@Component
public class CacheScheduler {
    private final MongoTemplate mongoTemplate;
    private final CacheService cacheService;
    private final Random random = new Random();
    private volatile boolean running = true;

    // 失败电影ID记录
    private final Set<Integer> failedMovieIds = new HashSet<>();
    private static final String FAILED_IDS_FILE = "failed_movies.txt";

    // 新增：最小ID配置（从配置文件读取）
    @Value("${movie.min-id:184}")
    private int minId;

    public CacheScheduler(MongoTemplate mongoTemplate, CacheService cacheService) {
        this.mongoTemplate = mongoTemplate;
        this.cacheService = cacheService;
        // 启动时加载历史失败记录
        loadFailedIds();
        log.info("配置的最小电影ID: {}", minId);
    }

    @Scheduled(cron = "0 26 20 * 6 ?", zone = "Asia/Shanghai")
    public void cacheAllMovieMetaData() {
        log.info("开始获取电影数据（失败重试: {}部）", failedMovieIds.size());
        log.info("只处理ID大于 {} 的电影", minId);
        long startTime = System.currentTimeMillis();

        // 获取需要处理的电影（只处理大于minId的电影）
        List<Movie> moviesToProcess = getMoviesToProcess();
        if (moviesToProcess.isEmpty()) {
            log.info("没有需要处理的电影");
            return;
        }

        // 按50个分组处理
        List<List<Movie>> groups = partition(moviesToProcess, 50);
        log.info("共 {} 部电影（ID > {}），分成 {} 组处理",
                moviesToProcess.size(), minId, groups.size());

        int groupCount = 0;
        for (List<Movie> group : groups) {
            groupCount++;
            log.info("===== 开始处理第 {} 组（共 {} 部） =====", groupCount, group.size());

            // 处理组内每个电影
            for (Movie movie : group) {
                processMovie(movie);
                if(!running) {
                    log.warn("处理被中断，停止后续操作");
                    return;
                }
                // 随机休眠8-13秒
                int sleepTime = 10000 + random.nextInt(5000);
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            // 每组处理完成后休眠2分钟
            if (groupCount < groups.size()) {
                log.info("===== 第 {} 组处理完成，等待2分钟后继续 =====", groupCount);
                try {
                    Thread.sleep(180_000); // 2分钟
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        log.info("===== 所有电影处理完成 =====");
        log.info("总耗时: {} 分钟", (System.currentTimeMillis() - startTime) / 60000);
        saveFailedIds();
        log.info("失败电影: {} 部", failedMovieIds.size());
    }

    // 获取需要处理的电影列表（只处理大于minId的电影）
    private List<Movie> getMoviesToProcess() {
        // 创建查询条件：ID大于minId
        Criteria criteria = Criteria.where("_id").gt(minId);

        if (failedMovieIds.isEmpty()) {
            // 第一次执行：获取所有大于minId的电影
            return mongoTemplate.find(new Query(criteria), Movie.class);
        } else {
            // 失败重试：只处理失败记录中大于minId的电影
            criteria = criteria.and("_id").in(failedMovieIds);
            return mongoTemplate.find(new Query(criteria), Movie.class);
        }
    }

    private void processMovie(Movie movie) {
        Integer movieId = movie.get_id();
        log.info("处理电影: {} - {}", movieId, movie.getTitle());

        try {
            Map<String, String> result = getInfoFromDouban(movie.getLink());

            // 提取并验证数据
            String rating = result.getOrDefault("rating", "暂无评分");
            String coverUrl = result.getOrDefault("cover", "");
            String summary = result.getOrDefault("summary", "");

            // 验证评分格式
            if (!rating.matches("\\d+\\.?\\d*")) {
                rating = "暂无评分";
            }

            // 保存到Elasticsearch
            cacheService.cacheMovieMeta(movieId, summary, rating, coverUrl);
            log.info("电影 {} 保存成功", movieId);

            // 从失败集合中移除
            failedMovieIds.remove(movieId);
        } catch (Exception e) {
            log.error("处理电影 {} 失败: {}", movieId, e.getMessage());
            // 停止定时任务
            running = false;
            failedMovieIds.add(movieId);
        }
    }

    // 调用Python脚本获取数据
    public static Map<String, String> getInfoFromDouban(String url) throws IOException, InterruptedException {
        Process process = null;
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "/usr/bin/python3",
                    "MovieNight/common/src/main/java/com/satti/python_utils/scrape.py",
                    url
            );
            pb.redirectErrorStream(true);
            process = pb.start();

            // 读取Python脚本输出
            StringBuilder jsonBuilder = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)))
            {
                String line;
                while ((line = reader.readLine()) != null) {
                    jsonBuilder.append(line);
                }
            }

            // 解析JSON
            return new ObjectMapper().readValue(
                    jsonBuilder.toString(),
                    new TypeReference<Map<String, String>>() {}
            );
        } finally {
            if (process != null && process.isAlive()) {
                process.destroy();
            }
        }
    }

    // 辅助方法：将列表分组
    private <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            partitions.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return partitions;
    }

    // 加载失败记录
    private void loadFailedIds() {
        try {
            Path path = Paths.get(FAILED_IDS_FILE);
            if (Files.exists(path)) {
                Files.readAllLines(path).forEach(line -> {
                    try {
                        int id = Integer.parseInt(line.trim());
                        // 只加载大于minId的失败记录
                        if (id > minId) {
                            failedMovieIds.add(id);
                        }
                    } catch (NumberFormatException ignored) {}
                });
            }
        } catch (IOException e) {
            log.warn("加载失败记录失败: {}", e.getMessage());
        }
    }

    // 保存失败记录
    private void saveFailedIds() {
        if (failedMovieIds.isEmpty()) return;

        try {
            Path path = Paths.get(FAILED_IDS_FILE);
            Files.write(
                    path,
                    failedMovieIds.stream().map(String::valueOf).toList(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );
        } catch (IOException e) {
            log.warn("保存失败记录失败: {}", e.getMessage());
        }
    }
}