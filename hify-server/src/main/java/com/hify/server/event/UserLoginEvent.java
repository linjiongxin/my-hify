package com.hify.server.event;

import com.hify.common.web.event.BaseApplicationEvent;
import lombok.Getter;

/**
 * 用户登录事件
 *
 * @author hify
 */
@Getter
public class UserLoginEvent extends BaseApplicationEvent {

    private final Long userId;
    private final String username;

    public UserLoginEvent(Object source, Long userId, String username) {
        super(source);
        this.userId = userId;
        this.username = username;
    }
}
