package com.hify.workflow.config;

import lombok.Data;

/**
 * 重试配置
 */
@Data
public class RetryConfig {

    /**
     * 最大重试次数
     */
    private int maxRetries = 3;

    /**
     * 重试间隔（秒）
     */
    private int retryIntervalSeconds = 3;
}