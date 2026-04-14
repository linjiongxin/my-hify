package com.hify.model.provider;

import com.hify.model.config.LlmGatewayProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

/**
 * LLM Provider 信号量管理器
 * <p>按 Provider 控制并发调用数</p>
 *
 * @author hify
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LlmProviderSemaphoreManager {

    private final LlmGatewayProperties properties;
    private final Map<String, Semaphore> semaphoreMap = new ConcurrentHashMap<>();

    private Semaphore getSemaphore(String providerCode) {
        return semaphoreMap.computeIfAbsent(providerCode, code -> {
            Integer max = properties.getProviderMaxConcurrent().get(code);
            int permits = max != null ? max : properties.getDefaultMaxConcurrent();
            log.info("初始化 Provider 信号量, code={}, permits={}", code, permits);
            return new Semaphore(permits);
        });
    }

    /**
     * 获取许可
     */
    public void acquire(String providerCode) throws InterruptedException {
        getSemaphore(providerCode).acquire();
    }

    /**
     * 释放许可
     */
    public void release(String providerCode) {
        getSemaphore(providerCode).release();
    }
}
