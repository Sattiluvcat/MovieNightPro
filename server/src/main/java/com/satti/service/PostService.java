package com.satti.service;

import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;

public interface PostService {
    /**
     * 清空Redis中的用户行为数据
     */
    ApplicationListener<ContextClosedEvent> shutdownListener();
}
