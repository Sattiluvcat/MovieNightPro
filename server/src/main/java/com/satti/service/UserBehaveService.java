package com.satti.service;

import jakarta.servlet.http.HttpSession;

public interface UserBehaveService {
    void viewEvent(Integer id, HttpSession session);

    void watchTogetherEvent(Integer id, HttpSession session);

    void rateEvent(Integer id, String rate,HttpSession session);
}
