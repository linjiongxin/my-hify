package com.hify.model.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * LLM 网关配置属性
 *
 * @author hify
 */
@Data
@Component
@ConfigurationProperties(prefix = "hify.llm.gateway")
public class LlmGatewayProperties {

    /**
     * 默认 Provider 并发限制
     */
    private Integer defaultMaxConcurrent = 10;

    /**
     * 各 Provider 并发限制（key=providerCode, value=并发数）
     */
    private Map<String, Integer> providerMaxConcurrent = new HashMap<>();

    /**
     * 各 Provider 配置（key=providerCode）
     */
    private Map<String, ProviderConfig> providers = new HashMap<>();

    @Data
    public static class ProviderConfig {
        /**
         * API 基础地址
         */
        private String apiBaseUrl;

        /**
         * API Key
         */
        private String apiKey;
    }
}
