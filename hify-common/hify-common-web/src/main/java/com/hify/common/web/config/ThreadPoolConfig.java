package com.hify.common.web.config;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 线程池配置
 *
 * <p>规范：
 * <ul>
 *   <li>核心线程数 = CPU * 2</li>
 *   <li>最大线程数 = CPU * 4</li>
 *   <li>队列容量 = 500</li>
 *   <li>拒绝策略 = CallerRunsPolicy</li>
 * </ul>
 * </p>
 *
 * @author hify
 */
@Slf4j
@Configuration
public class ThreadPoolConfig {

    /**
     * CPU 核心数
     */
    private static final int CPU_CORES = Runtime.getRuntime().availableProcessors();

    /**
     * 核心线程数
     */
    private static final int CORE_POOL_SIZE = CPU_CORES * 2;

    /**
     * 最大线程数
     */
    private static final int MAX_POOL_SIZE = CPU_CORES * 4;

    /**
     * 队列容量
     */
    private static final int QUEUE_CAPACITY = 500;

    /**
     * 线程空闲存活时间（秒）
     */
    private static final long KEEP_ALIVE_SECONDS = 60L;

    /**
     * 通用业务线程池
     * <p>用于：异步任务、批量处理等</p>
     */
    @Bean("commonExecutor")
    @Primary
    public ThreadPoolExecutor commonExecutor() {
        log.info("初始化通用线程池: core={}, max={}, queue={}", CORE_POOL_SIZE, MAX_POOL_SIZE, QUEUE_CAPACITY);

        return new ThreadPoolExecutor(
                CORE_POOL_SIZE,
                MAX_POOL_SIZE,
                KEEP_ALIVE_SECONDS,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(QUEUE_CAPACITY),
                new ThreadFactoryBuilder()
                        .setNameFormat("common-pool-%d")
                        .setUncaughtExceptionHandler((t, e) ->
                                log.error("线程异常, thread={}", t.getName(), e))
                        .build(),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    /**
     * LLM 调用线程池
     * <p>用于：异步 LLM API 调用</p>
     */
    @Bean("llmExecutor")
    public ThreadPoolExecutor llmExecutor() {
        int coreSize = Math.max(4, CPU_CORES);
        int maxSize = coreSize * 2;

        log.info("初始化LLM线程池: core={}, max={}, queue={}", coreSize, maxSize, QUEUE_CAPACITY);

        return new ThreadPoolExecutor(
                coreSize,
                maxSize,
                KEEP_ALIVE_SECONDS,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(QUEUE_CAPACITY),
                new ThreadFactoryBuilder()
                        .setNameFormat("llm-pool-%d")
                        .setUncaughtExceptionHandler((t, e) ->
                                log.error("LLM线程异常, thread={}", t.getName(), e))
                        .build(),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    /**
     * SSE 流式线程池
     * <p>用于：SSE 流式响应处理</p>
     */
    @Bean("sseExecutor")
    public ThreadPoolExecutor sseExecutor() {
        int coreSize = Math.max(4, CPU_CORES);
        int maxSize = coreSize * 2;

        log.info("初始化SSE线程池: core={}, max={}, queue={}", coreSize, maxSize, QUEUE_CAPACITY);

        return new ThreadPoolExecutor(
                coreSize,
                maxSize,
                KEEP_ALIVE_SECONDS,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(QUEUE_CAPACITY),
                new ThreadFactoryBuilder()
                        .setNameFormat("sse-pool-%d")
                        .setUncaughtExceptionHandler((t, e) ->
                                log.error("SSE线程异常, thread={}", t.getName(), e))
                        .build(),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

}
