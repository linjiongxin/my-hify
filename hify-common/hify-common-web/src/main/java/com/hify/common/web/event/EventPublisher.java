package com.hify.common.web.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 事件发布器
 *
 * @author hify
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * 发布事件
     */
    public void publish(BaseApplicationEvent event) {
        log.debug("发布事件: {}, source={}", event.getClass().getSimpleName(), event.getSource());
        applicationEventPublisher.publishEvent(event);
    }
}
