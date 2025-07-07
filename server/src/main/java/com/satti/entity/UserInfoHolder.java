package com.satti.entity;

import lombok.extern.slf4j.Slf4j;

/**
 * 管理ThreadLocal中的用户信息——最后没用到
 */
@Slf4j
public class UserInfoHolder {
    private static final ThreadLocal<UserInfo> USER_INFO_THREAD_LOCAL = new ThreadLocal<>();

    public static void set(UserInfo userInfo) {
        log.info(userInfo.toString());
        USER_INFO_THREAD_LOCAL.set(userInfo);
    }

    public static UserInfo get() {
//        log.info(USER_INFO_THREAD_LOCAL.get().toString());
        return USER_INFO_THREAD_LOCAL.get();
    }

    public static void clear() {
        USER_INFO_THREAD_LOCAL.remove();
    }
}
