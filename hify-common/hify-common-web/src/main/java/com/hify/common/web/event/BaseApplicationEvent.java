package com.hify.common.web.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

/**
 * 应用事件基类
 *
 * @author hify
 */
@Getter
public abstract class BaseApplicationEvent extends ApplicationEvent {

    /**
     * 事件发生时间
     */
    private final LocalDateTime eventTime;

    public BaseApplicationEvent(Object source) {
        super(source);
        this.eventTime = LocalDateTime.now();
    }
}
