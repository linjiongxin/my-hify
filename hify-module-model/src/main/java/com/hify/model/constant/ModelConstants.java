package com.hify.model.constant;

/**
 * 模型模块常量
 *
 * @author hify
 */
public final class ModelConstants {

    private ModelConstants() {
        // 禁止实例化
    }

    /**
     * 协议类型
     */
    public static final class ProtocolType {
        public static final String OPENAI_COMPATIBLE = "openai_compatible";
    }

    /**
     * 鉴权类型
     */
    public static final class AuthType {
        public static final String BEARER = "BEARER";
        public static final String API_KEY = "API_KEY";
        public static final String NONE = "NONE";
        public static final String CUSTOM = "CUSTOM";
    }

    /**
     * HTTP 请求头名称
     */
    public static final class HeaderName {
        public static final String AUTHORIZATION = "Authorization";
        public static final String API_KEY = "api-key";
    }

    /**
     * 鉴权配置 JSON 中的键名
     */
    public static final class AuthConfigKey {
        public static final String HEADER_NAME = "headerName";
        public static final String PREFIX = "prefix";
        public static final String HEADERS = "headers";
        public static final String API_KEY = "apiKey";
    }

    /**
     * 健康状态
     */
    public static final class HealthStatus {
        public static final String HEALTHY = "healthy";
        public static final String DEGRADED = "degraded";
        public static final String UNHEALTHY = "unhealthy";
        public static final String UNKNOWN = "unknown";
    }
}
