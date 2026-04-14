package com.hify.common.web.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 指标埋点辅助类
 *
 * @author hify
 */
@Component
@RequiredArgsConstructor
public class MetricsHelper {

    private final MeterRegistry meterRegistry;

    /**
     * 记录 LLM 请求耗时
     */
    public void recordLlmRequest(String provider, String modelId, long durationMs) {
        Timer.builder("llm.request.duration")
                .tag("provider", provider)
                .tag("model", modelId)
                .register(meterRegistry)
                .record(durationMs, TimeUnit.MILLISECONDS);
    }

    /**
     * 记录 LLM Token 消耗
     */
    public void recordLlmTokens(String provider, String modelId, long promptTokens, long completionTokens) {
        meterRegistry.counter("llm.tokens.prompt", "provider", provider, "model", modelId)
                .increment(promptTokens);
        meterRegistry.counter("llm.tokens.completion", "provider", provider, "model", modelId)
                .increment(completionTokens);
    }

    /**
     * 记录 LLM 请求次数
     */
    public void countLlmRequest(String provider, String modelId, boolean success) {
        Counter.builder("llm.request.total")
                .tag("provider", provider)
                .tag("model", modelId)
                .tag("status", success ? "success" : "failure")
                .register(meterRegistry)
                .increment();
    }

    /**
     * 记录 LLM 限流排队数
     */
    public void recordLlmSemaphoreWaiting(String provider, int waiting) {
        meterRegistry.gauge("llm.semaphore.waiting",
                io.micrometer.core.instrument.Tags.of("provider", provider),
                waiting);
    }
}
