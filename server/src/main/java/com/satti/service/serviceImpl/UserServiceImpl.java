package com.satti.service.serviceImpl;

import com.satti.entity.User;
import com.satti.entity.UserInfo;
import com.satti.mapper.UserMapper;
import com.satti.mapper.UserScoreMapper;
import com.satti.result.Result;
import com.satti.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private UserScoreMapper userScoreMapper;

    @Override
    public boolean isEmailRegistered(String contact) {
        /**
         * 由数据库处理信息         *
         * @result true: 已注册
         */
        return userMapper.isEmailRegistered(contact) != null;
    }

    @Override
    public void registerUser(UserInfo userInfo) {
        String nickname = userInfo.getNickname();
        // 密码加密
        userInfo.encryptPassword(passwordEncoder);
        String password = userInfo.getPassword();

        String contact = userInfo.getContact();
        String title = userInfo.getTitle();
        // 存入已被加密的密码
        userMapper.registerUser(nickname, password, contact, title);
    }

    @Override
    public Result checkInUser(User user, HttpSession session) {
        /**
         * @result 返回Result类型，若为true则填进session
         * @util 检查用户是否存在 & 密码是否正确
         */
        UserInfo userInfo= new UserInfo();
        BeanUtils.copyProperties(user, userInfo);
        System.out.println(userInfo.getPassword());
        userInfo.encryptPassword(passwordEncoder);
        String password=userMapper.checkInUser(userInfo.getContact());
        if(password==null){
            return Result.failure("用户不存在，请注册");
        }
        System.out.println(password+" "+userInfo.getPassword());
        if(password.equals(userInfo.getPassword())){
            userInfo=userMapper.getInfoByContact(user.getContact());
            session.setAttribute("userInfo", userInfo);
            return Result.success(userInfo);
        }
        return Result.failure("密码错误");
    }

    @Override
    public boolean rateWithTitle(Integer id, String rate, HttpSession session) {
        System.out.println(rate.charAt(11));
        double rating = Double.parseDouble(String.valueOf(rate.charAt(11)));
        if(rate.charAt(12)=='0'){
            rating=10.0;
        }
        UserInfo userInfo = (UserInfo) session.getAttribute("userInfo");
        if(userInfo==null){
            return false;
        }
        userScoreMapper.setScore(userInfo.getContact(),id,rating);
        return true;
    }
}
