package com.satti.service;

import com.satti.entity.User;
import com.satti.entity.UserInfo;
import com.satti.result.Result;
import jakarta.servlet.http.HttpSession;

public interface UserService {
    boolean isEmailRegistered(String contact);

    void registerUser(UserInfo userInfo);

    Result checkInUser(User user, HttpSession session);

    boolean rateWithTitle(Integer id, String rate, HttpSession session);
}
