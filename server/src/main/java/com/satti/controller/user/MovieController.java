package com.satti.controller.user;

import com.satti.entity.*;
import com.satti.result.PageResult;
import com.satti.service.MailService;
import com.satti.service.MovieService;
import com.satti.service.UserBehaveService;
import com.satti.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.satti.result.Result;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/user/movie")
@Slf4j
public class MovieController {
    @Autowired
    private MovieService movieService;
    @Autowired
    private MailService mailService;
    @Autowired
    private UserService userService;
    @Autowired
    private UserBehaveService userBehaveService;

    @GetMapping("/hello")
    public String hello(){
        System.out.println("Hello World!");
        return "hello";
    }

    @PostMapping("/register")
    public Result register(@RequestBody UserInfo userInfo) {
        log.info("REGISTER USER! nickname: {} contact: {}", userInfo.getNickname(),userInfo.getContact());
        // 1st: 检查邮箱是否已注册
        if (userService.isEmailRegistered(userInfo.getContact())) {
            return Result.failure("邮箱已被注册，请更换邮箱");
        }
// 发送验证码
//        String code = mailService.sendVerificationCode(userInfo.getContact());
//        session.setAttribute("verificationCode", code);
// 返回验证码给前端
        // 注册用户
        userService.registerUser(userInfo);
        return Result.success();
    }

    // 4th: 用户登录后记录信息
    @PostMapping("/login")
    public Result login(@RequestBody User user, HttpSession session) {
        log.info("CHECK USER! their contact: {}", user.getContact());
        if(user.getContact().isEmpty()||user.getPassword().isEmpty()){
            return Result.failure("邮箱或密码不能为空");
        }
        // 检查用户存在 & 正确密码
        return userService.checkInUser(user, session);
    }

    // 00th: 电影分页查询 前端懒得实现
    @GetMapping("/page")
    public PageResult paginatedSearch(@RequestParam List<MovieBigRow> movieBigRows,
                                                      @RequestParam int page,
                                                      @RequestParam int size) {
        int total = movieBigRows.size();
        int start = (page - 1) * size;
        int end = Math.min(start + size, total);
        List<MovieBigRow> paginatedMovies = movieBigRows.subList(start, end);
        return new PageResult<>(total, paginatedMovies, page, size);
    }

    // 0th: 电影具体搜索
    @GetMapping("/search")
    public Result<List<MovieBigRow>> searchMovies(@RequestParam String keyword){
        log.info("searching movies with keyword: {}", keyword);
        List<MovieBigRow> movieBigRows =movieService.searchMovies(keyword);
        return Result.success(movieBigRows);
    }

    // 电影模糊搜索（简介）
    @GetMapping("/fuzzySearch")
    public Result<List<MovieBigRow>> fuzzySearch(@RequestParam String keyword){
        log.info("fuzzy searching movies with keyword: {}", keyword);
        List<MovieBigRow> movieBigRows = movieService.searchDescriptions(keyword);
        return Result.success(movieBigRows);
    }

    // 分类与语言同时搜索
    @GetMapping("/categorySearch")
    public Result<List<MovieBigRow>> categorySearch(@RequestParam String tags,
                                                    @RequestParam String languages) {
        // 处理中文编码问题 此处解码 其实可以不解码 调试的时候解码
        log.info("searching movies with tags: {}, languages: {}", tags, languages);
        String[] tagList = null;
        String[] languageList = null;
        // 此处不能直接split(",") 会造成空字符串问题（length!=0）
        if(!tags.isEmpty())
            tagList = tags.split(",").clone();
        if(!languages.isEmpty())
            languageList = languages.split(",").clone();
        List<MovieBigRow> movieBigRows = movieService.categorySearch(tagList, languageList);
        return Result.success(movieBigRows);
    }

    // 主页排行榜与每日推荐功能
    @GetMapping("/home")
    public CompletableFuture<Result<List<MovieSmallRow>>> getHomePage(HttpSession session) {
        log.info("加载排行榜与每日推荐");
        if(session.getAttribute("userInfo") == null) {
            log.info("用户未登录，无法加载主页数据");
            return CompletableFuture.completedFuture(Result.failure("请先登录"));
        }
        return movieService.loadHomeAsync(session)
                .thenApply(Result::success)
                .exceptionally(ex -> {
                    log.error("Error loading home page data");
                    return Result.failure("加载主页失败，请稍后再试");
                });
    }

    @GetMapping("/find-all")
    public Result<List<MovieBigRow>> getAllMovies() {
        log.info("getting all movies");
        List<MovieBigRow> movieBigRows = movieService.getAllMovies();
        return Result.success(movieBigRows);
    }

    @GetMapping("/suggest")
    public Result<List<MovieBigRow>> getSuggestedMovies() {
        log.info("getting suggested movies");
        List<MovieBigRow> movieBigRows = movieService.getSuggestedMovies();
        return Result.success(movieBigRows);
    }

    // 电影详情
    @GetMapping("/{_id}")
    public Result<MovieDTO> movieDetails(@PathVariable String _id, HttpSession session) {
        log.info("getting movie details with id: {}", _id);
        // 不判断数字 直接转换（前端限制）
        Integer id = Integer.parseInt(_id);
        // 用户评分的调取与显示
        MovieDTO movieDTO = movieService.getMovieDetails(id, session);
        userBehaveService.viewEvent(id, session); // 记录用户观看事件
//        System.out.println(movieDTO.getSummary());
        return Result.success(movieDTO);
    }

    // 一起看
    @PostMapping("/{id}/watch-together")
    public Result watchTogether(@PathVariable Integer id, HttpSession session) {
        log.info("watching together with movie id: {}", id);
        boolean flag=movieService.watchTogetherWithTitle(id, session);
        userBehaveService.watchTogetherEvent(id,session);
        if (!flag)
            return Result.failure("请登录后再操作噢");
        else
            return Result.success();
    }


    // 评论——和一起看一样，没准备放到数据库里(暂时)
    @PostMapping("/{id}/comment")
    public Result comment(@PathVariable Integer id, @RequestBody String comment, HttpSession session) {
        log.info("commenting on movie id: {}", id);
        boolean flag=movieService.commentWithTitle(id, comment, session);
        if (!flag)
            return Result.failure("请登录后再操作噢");
        else
            return Result.success();
    }

    // 评分
    @PostMapping("/{id}/rate")
    public Result rateMovie(@PathVariable Integer id, @RequestBody String rate, HttpSession session) {
        log.info("rating movie id: {} with rate: {}", id, rate);
        boolean flag=userService.rateWithTitle(id, rate, session);
        userBehaveService.rateEvent(id,rate,session);
        if (!flag)
            return Result.failure("评分出错了，请稍后再试");
        else
            return Result.success();
    }

    // 登出
    @PostMapping("/logout")
    public Result logout(HttpSession session) {
        log.info("logging out");
        session.invalidate();
        return Result.success();
    }

    // last:管理端的增删改权限 现在直接改MongoDB即可 之后可以做细化（主要是前端不咋会）

}
