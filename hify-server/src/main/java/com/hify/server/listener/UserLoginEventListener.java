package com.hify.server.listener;

import com.hify.server.event.UserLoginEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 用户登录事件监听器
 *
 * @author hify
 */
@Slf4j
@Component
public class UserLoginEventListener {

    @Async("commonExecutor")
    @EventListener
    public void onUserLogin(UserLoginEvent event) {
        log.info("用户登录事件处理, userId={}, username={}, eventTime={}",
                event.getUserId(), event.getUsername(), event.getEventTime());
        // 此处可扩展：记录登录日志、发送通知等
    }
}
