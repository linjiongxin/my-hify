package com.hify.common.core.constants;

/**
 * 缓存常量
 * <p>Redis Key 前缀定义</p>
 *
 * @author hify
 */
public final class CacheConstants {

    private CacheConstants() {
        throw new UnsupportedOperationException("常量类禁止实例化");
    }

    /**
     * 全局前缀
     */
    public static final String KEY_PREFIX = "hify:";

    /**
     * 分隔符
     */
    public static final String KEY_SEPARATOR = ":";

    // ==================== 会话缓存 ====================

    /**
     * 用户会话
     * hify:session:{token}
     */
    public static final String SESSION_USER = KEY_PREFIX + "session";

    /**
     * 用户令牌
     * hify:user:token:{userId}
     */
    public static final String USER_TOKEN = KEY_PREFIX + "user:token";

    // ==================== SSE 流式缓存 ====================

    /**
     * SSE 会话
     * hify:sse:session:{sessionId}
     */
    public static final String SSE_SESSION = KEY_PREFIX + "sse:session";

    /**
     * SSE 流式数据缓冲
     * hify:sse:buffer:{sessionId}
     */
    public static final String SSE_BUFFER = KEY_PREFIX + "sse:buffer";

    // ==================== 限流缓存 ====================

    /**
     * 接口限流
     * hify:rate:api:{path}
     */
    public static final String RATE_LIMIT_API = KEY_PREFIX + "rate:api";

    /**
     * 用户限流
     * hify:rate:user:{userId}
     */
    public static final String RATE_LIMIT_USER = KEY_PREFIX + "rate:user";

    // ==================== 模型缓存 ====================

    /**
     * 模型提供商列表
     * hify:model:providers
     */
    public static final String MODEL_PROVIDERS = KEY_PREFIX + "model:providers";

    /**
     * 模型列表
     * hify:model:list:{providerId}
     */
    public static final String MODEL_LIST = KEY_PREFIX + "model:list";

    // ==================== RAG 缓存 ====================

    /**
     * 知识库文档
     * hify:rag:doc:{docId}
     */
    public static final String RAG_DOCUMENT = KEY_PREFIX + "rag:doc";

    // ==================== 锁相关 ====================

    /**
     * 分布式锁前缀
     * hify:lock:{resource}
     */
    public static final String LOCK_PREFIX = KEY_PREFIX + "lock";

    /**
     * 构建完整的缓存 Key
     *
     * @param prefix Key 前缀
     * @param args   参数
     * @return 完整 Key
     */
    public static String buildKey(String prefix, Object... args) {
        StringBuilder sb = new StringBuilder(prefix);
        for (Object arg : args) {
            sb.append(KEY_SEPARATOR).append(arg);
        }
        return sb.toString();
    }

}
