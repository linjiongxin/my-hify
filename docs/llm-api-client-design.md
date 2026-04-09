# LLM API 客户端设计方案

> 多 Provider（OpenAI、Claude、Gemini、Ollama）调用的线程管理、容错、超时、重试方案
>
> 整理时间: 2026-04-09

---

## 一、整体架构

```
┌─────────────────────────────────────────────────────────────┐
│                    Controller/Service                        │
└─────────────────────────┬───────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────┐
│                  LLM Gateway Service                         │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              Provider 级信号量限流（动态）              │   │
│  │   OpenAI:20 │ DeepSeek:10 │ Ollama:5 │ ...（用户配置）  │   │
│  └─────────────────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────────┐   │
│  │           Circuit Breaker（按 Provider）              │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────┬───────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────┐
│              统一线程池（全局共享）                            │
│         核心：CPU * 2，最大：CPU * 4，队列：500              │
└─────────────────────────┬───────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────┐
│         共享 HTTP 连接池（按 Host 复用）                       │
│         最大总连接：200，单 Host 最大：50                     │
└─────────────────────────┬───────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────┐
│              Retry + Timeout + Fallback                     │
└─────────────────────────────────────────────────────────────┘
```

### 核心设计变更

| 原方案 | 新方案 | 原因 |
|--------|--------|------|
| 每 Provider 一个线程池 | **统一线程池** | 用户可配几十个模型，线程数不可控 |
| 每 Provider 一个连接池 | **共享连接池** | 按 Host 复用，减少资源占用 |

---

## 二、线程管理

### 2.1 设计原则

| 原则 | 说明 |
|------|------|
| **统一性** | 全局共享线程池，无论多少个 Provider 线程数固定 |
| **分级限流** | 线程池保护系统，信号量保护单个 Provider |
| **动态性** | Provider 配置热加载，信号量动态创建 |

### 2.2 统一线程池配置

```java
@Configuration
public class LlmExecutionConfig {

    @Bean("llmExecutor")
    public ThreadPoolTaskExecutor llmExecutor() {
        int processors = Runtime.getRuntime().availableProcessors();
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(processors * 2);
        executor.setMaxPoolSize(processors * 4);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("llm-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);

        // 全局监控
        meterRegistry.gauge("llm.executor.active",
            executor.getThreadPoolExecutor(),
            ThreadPoolExecutor::getActiveCount);
        meterRegistry.gauge("llm.executor.queue.size",
            executor.getThreadPoolExecutor(),
            e -> e.getQueue().size());

        return executor;
    }
}
```

### 2.3 Provider 级信号量限流

```java
@Component
public class ProviderRateLimiter {

    private final ConcurrentHashMap<String, Semaphore> semaphores = new ConcurrentHashMap<>();
    private final LlmClientProperties properties;
    private final ThreadPoolTaskExecutor llmExecutor;

    /**
     * 带限流的异步执行
     * 每个 Provider 独立的信号量，动态创建
     */
    public <T> CompletableFuture<T> withRateLimit(
            String providerKey,
            Supplier<CompletableFuture<T>> supplier) {

        return CompletableFuture.supplyAsync(() -> {
            Semaphore semaphore = semaphores.computeIfAbsent(providerKey, key -> {
                int limit = properties.getProvider(key).getMaxConcurrentRequests();
                return new Semaphore(limit);
            });

            if (!semaphore.tryAcquire(30, TimeUnit.SECONDS)) {
                throw new LlmRateLimitException(
                    "Provider " + providerKey + " 并发限流等待超时");
            }

            try {
                return supplier.get().join();
            } finally {
                semaphore.release();
            }
        }, llmExecutor);
    }
}
```
```

---

## 三、HTTP 客户端与连接池

### 3.1 共享 OkHttpClient 配置

```java
@Configuration
public class LlmHttpClientConfig {

    @Bean
    public OkHttpClient sharedLlmHttpClient(LlmClientProperties properties) {
        return new OkHttpClient.Builder()
            // 全局连接池：所有 Provider 共享
            .connectionPool(new ConnectionPool(200, 5, TimeUnit.MINUTES))
            // 全局调度器：按 Host 限制并发
            .dispatcher(new Dispatcher().apply {
                setMaxRequests(200);        // 全局总并发
                setMaxRequestsPerHost(50);  // 单 Host 限制
            })
            // 默认超时（可被请求级覆盖）
            .connectTimeout(Duration.ofSeconds(5))
            .readTimeout(Duration.ofSeconds(60))
            .callTimeout(Duration.ofSeconds(90))
            .retryOnConnectionFailure(false)
            .build();
    }
}
```

### 3.2 连接池参数说明

| 参数 | 推荐值 | 说明 |
|------|--------|------|
| **maxIdleConnections** | 200 | 全局最大空闲连接，所有 Provider 共享 |
| **keepAliveDuration** | 5min | 连接保活时间，减少 TLS 握手 |
| **maxRequests** | 200 | 全局最大并发请求数 |
| **maxRequestsPerHost** | 50 | 单 Host 最大并发（保护单个 Provider）|

---

## 四、超时控制（三层防护）

### 4.1 三层超时模型

```
┌────────────────────────────────────────────────────┐
│  Layer 3: 业务层 Token 级超时 (流式响应)              │
│  - 每 N 秒必须有数据输出                              │
│  - 防止长连接假死                                     │
├────────────────────────────────────────────────────┤
│  Layer 2: CompletableFuture 超时                      │
│  - 异步任务总超时控制                                  │
│  - 兜底保护                                          │
├────────────────────────────────────────────────────┤
│  Layer 1: OkHttp 调用超时                              │
│  - connectTimeout: 5s                               │
│  - readTimeout: 60s                                 │
│  - callTimeout: 90s (整体调用)                        │
└────────────────────────────────────────────────────┘
```

### 4.2 实现代码

```java
@Component
public class TimeoutController {

    /**
     * 三层超时防护
     */
    public <T> CompletableFuture<T> withTimeout(
            CompletableFuture<T> future,
            Duration timeout,
            String operation) {

        return future.orTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
            .exceptionally(ex -> {
                if (ex instanceof TimeoutException) {
                    throw new LlmTimeoutException("LLM request timeout: " + operation, ex);
                }
                throw new LlmException("LLM request failed: " + operation, ex);
            });
    }

    /**
     * 流式响应健康检查：每 N 秒必须有数据
     */
    public void validateStreamHealth(long lastDataTimeMs, long maxIdleMs) {
        long idleTime = System.currentTimeMillis() - lastDataTimeMs;
        if (idleTime > maxIdleMs) {
            throw new LlmStreamTimeoutException(
                "Stream idle for " + idleTime + "ms, max allowed: " + maxIdleMs);
        }
    }
}
```

### 4.3 各 Provider 超时配置

| Provider | connect | read | total | 说明 |
|----------|---------|------|-------|------|
| OpenAI | 5s | 60s | 90s | 标准配置 |
| DeepSeek | 5s | 60s | 90s | 国产 API，同标准 |
| Ollama(本地) | 2s | 300s | 600s | 本地模型可能很慢 |

---

## 五、重试策略

### 5.1 可重试 vs 不可重试异常

```
可重试 (Retryable):
├── LlmServerException (5xx)
├── SocketTimeoutException
└── LlmConnectionException

不可重试 (Non-retryable):
├── LlmClientException (4xx, 参数错误)
├── LlmRateLimitException (429, 特殊处理)
└── LlmAuthException (401/403)
```

### 5.2 Spring Retry 配置

```java
@Service
public class LlmRetryService {

    /**
     * 标准重试：指数退避
     */
    @Retryable(
        retryFor = {
            LlmServerException.class,
            SocketTimeoutException.class,
            LlmConnectionException.class
        },
        noRetryFor = {
            LlmClientException.class,
            LlmRateLimitException.class
        },
        maxAttempts = 3,
        backoff = @Backoff(
            delay = 1000,       // 首次延迟 1s
            multiplier = 2,     // 指数退避
            maxDelay = 10000    // 最大延迟 10s
        )
    )
    public String callWithRetry(LlmRequest request, String providerKey) {
        return doCall(request, providerKey);
    }

    /**
     * 限流重试：更长延迟
     */
    @Retryable(
        retryFor = LlmRateLimitException.class,
        maxAttempts = 5,
        backoff = @Backoff(
            delay = 5000,
            multiplier = 3,
            maxDelay = 60000    // 最大 60s
        )
    )
    public String callWithRateLimitRetry(LlmRequest request, String providerKey) {
        return doCall(request, providerKey);
    }

    /**
     * 兜底：切换到备用 Provider
     */
    @Recover
    public String recover(LlmException ex, LlmRequest request, String providerKey) {
        String fallback = getFallbackProvider(providerKey);
        if (fallback != null) {
            return callWithRetry(request, fallback);
        }
        throw new LlmUnavailableException("All providers unavailable", ex);
    }
}
```

### 5.3 幂等性保证

```java
@Component
public class IdempotencyManager {

    private final ConcurrentHashMap<String, Long> processedKeys = new ConcurrentHashMap<>();

    /**
     * 生成幂等 Key：userId + conversationId + messageIndex
     */
    public String generateKey(String userId, String conversationId, int messageIndex) {
        return String.format("%s:%s:%d", userId, conversationId, messageIndex);
    }

    public boolean isProcessed(String key) {
        return processedKeys.containsKey(key);
    }

    public void markProcessed(String key) {
        processedKeys.put(key, System.currentTimeMillis());
    }
}
```

---

## 六、熔断器（Resilience4j）

### 6.1 熔断策略

```java
@Component
public class LlmCircuitBreakerManager {

    public CircuitBreaker getBreaker(String providerKey) {
        return breakers.computeIfAbsent(providerKey, key -> {
            CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                // 触发条件
                .failureRateThreshold(50)           // 失败率 50% 触发熔断
                .slowCallRateThreshold(50)          // 慢调用率 50% 触发
                .slowCallDurationThreshold(Duration.ofSeconds(10))

                // 恢复策略
                .waitDurationInOpenState(Duration.ofSeconds(30))  // 熔断 30s
                .permittedNumberOfCallsInHalfOpenState(3)         // 半开试 3 个

                // 统计窗口
                .slidingWindowSize(10)

                // 异常分类
                .recordExceptions(
                    LlmServerException.class,
                    LlmTimeoutException.class,
                    LlmConnectionException.class
                )
                .ignoreExceptions(LlmClientException.class)
                .build();

            return registry.circuitBreaker(key, config);
        });
    }
}
```

### 6.2 熔断状态流转

```
CLOSED (正常)
    │
    │ 失败率 > 50% 或 慢调用率 > 50%
    ▼
OPEN (熔断，拒绝请求)
    │
    │ 等待 30s
    ▼
HALF_OPEN (允许少量请求试探)
    │
    ├── 成功次数 >= 3 ──► CLOSED
    │
    └── 失败 ──► OPEN
```

---

## 七、完整调用链路

```java
@Service
@RequiredArgsConstructor
public class LlmApiClient {

    private final OkHttpClient httpClient;
    private final ProviderRateLimiter rateLimiter;
    private final LlmCircuitBreakerManager breakerManager;
    private final TimeoutController timeoutController;
    private final ThreadPoolTaskExecutor llmExecutor;

    /**
     * 完整调用链路：
     * 熔断 -> 信号量限流 -> 统一线程池 -> 超时控制
     */
    public CompletableFuture<LlmResponse> callAsync(
            String providerKey,
            LlmRequest request,
            Duration timeout) {

        CircuitBreaker breaker = breakerManager.getBreaker(providerKey);

        Supplier<CompletableFuture<LlmResponse>> decorated =
            Decorators.ofSupplier(() ->
                rateLimiter.withRateLimit(providerKey, () ->
                    executeCall(providerKey, request)
                )
            )
            .withCircuitBreaker(breaker)
            .withFallback(ex -> handleFallback(providerKey, request, ex))
            .decorate();

        return timeoutController.withTimeout(decorated.get(), timeout, providerKey);
    }

    private CompletableFuture<LlmResponse> executeCall(String providerKey, LlmRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            Request httpRequest = buildHttpRequest(providerKey, request);

            try (Response response = httpClient.newCall(httpRequest).execute()) {
                return parseResponse(response);
            }
        }, llmExecutor);
    }
}
```

### 调用流程说明

```
用户请求
    │
    ▼
┌────────────────────────────────────────┐
│ 1. Circuit Breaker 检查                │
│    - OPEN: 直接降级                     │
│    - CLOSED/HALF_OPEN: 继续执行          │
└────────────────────────────────────────┘
    │
    ▼
┌────────────────────────────────────────┐
│ 2. ProviderRateLimiter 信号量获取       │
│    - 获取成功: 继续执行                  │
│    - 等待超时: 抛出限流异常               │
└────────────────────────────────────────┘
    │
    ▼
┌────────────────────────────────────────┐
│ 3. 提交到统一线程池 (llmExecutor)        │
│    - 线程数固定，与 Provider 数量无关      │
└────────────────────────────────────────┘
    │
    ▼
┌────────────────────────────────────────┐
│ 4. HTTP 调用（共享连接池）               │
│    - 连接按 Host 复用                    │
└────────────────────────────────────────┘
    │
    ▼
┌────────────────────────────────────────┐
│ 5. Timeout 控制                        │
│    - 整体调用超时: 90s                   │
│    - 流式空闲超时: 30s                   │
└────────────────────────────────────────┘
```

---

## 八、流式响应实现（SSE）

### 8.1 技术选型：Spring MVC + SseEmitter

**不引入 WebFlux**，原因：
- 团队规模小（20-50人），保守选型优先
- 现有技术栈（Spring Boot + MyBatis-Plus）是同步生态
- SseEmitter 支撑 1000+ 并发 SSE 连接足够

### 8.2 Controller 层实现

```java
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatStreamService chatStreamService;

    @GetMapping(value = "/stream/{sessionId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(
            @PathVariable String sessionId,
            @RequestParam String message) {

        // 0 表示不超时，由业务层控制
        SseEmitter emitter = new SseEmitter(0L);

        // 启动流式处理
        chatStreamService.streamChat(sessionId, message, new StreamCallback() {
            @Override
            public void onNext(String content) {
                try {
                    emitter.send(SseEmitter.event()
                        .id(UUID.randomUUID().toString())
                        .name("message")
                        .data(Map.of(
                            "content", content,
                            "timestamp", System.currentTimeMillis()
                        )));
                } catch (IOException e) {
                    emitter.completeWithError(e);
                }
            }

            @Override
            public void onComplete() {
                try {
                    emitter.send(SseEmitter.event()
                        .name("done")
                        .data(Map.of("status", "completed")));
                } catch (IOException ignored) {}
                emitter.complete();
            }

            @Override
            public void onError(Throwable error) {
                try {
                    emitter.send(SseEmitter.event()
                        .name("error")
                        .data(Map.of("message", error.getMessage())));
                } catch (IOException ignored) {}
                emitter.completeWithError(error);
            }
        });

        // 连接生命周期管理
        emitter.onCompletion(() -> chatStreamService.stopStream(sessionId));
        emitter.onTimeout(() -> chatStreamService.stopStream(sessionId));
        emitter.onError((e) -> chatStreamService.stopStream(sessionId));

        return emitter;
    }
}
```

### 8.3 Service 层流式处理

```java
@Service
@RequiredArgsConstructor
public class ChatStreamService {

    private final LlmApiClient llmClient;
    private final ProviderRateLimiter rateLimiter;
    private final TimeoutController timeoutController;

    // 记录活跃会话，用于主动中断
    private final ConcurrentHashMap<String, CompletableFuture<Void>> activeStreams
        = new ConcurrentHashMap<>();

    public void streamChat(String sessionId, String message, StreamCallback callback) {
        LlmRequest request = buildRequest(sessionId, message);

        CompletableFuture<Void> streamFuture = llmClient.streamChat(
            request.getProviderKey(),
            request,
            new LlmStreamCallback() {
                private long lastDataTime = System.currentTimeMillis();

                @Override
                public void onChunk(String chunk) {
                    // 流式健康检查
                    timeoutController.validateStreamHealth(lastDataTime, 30000);
                    lastDataTime = System.currentTimeMillis();

                    callback.onNext(chunk);
                }

                @Override
                public void onComplete() {
                    activeStreams.remove(sessionId);
                    callback.onComplete();
                }

                @Override
                public void onError(Throwable error) {
                    activeStreams.remove(sessionId);
                    callback.onError(error);
                }
            }
        );

        activeStreams.put(sessionId, streamFuture);
    }

    public void stopStream(String sessionId) {
        CompletableFuture<Void> future = activeStreams.remove(sessionId);
        if (future != null && !future.isDone()) {
            future.cancel(true);
        }
    }
}

/**
 * 流式回调接口
 */
public interface StreamCallback {
    void onNext(String content);
    void onComplete();
    void onError(Throwable error);
}
```

### 8.4 LLM 客户端流式接口扩展

```java
@Service
@RequiredArgsConstructor
public class LlmApiClient {

    private final OkHttpClient httpClient;
    private final ProviderRateLimiter rateLimiter;
    private final LlmCircuitBreakerManager breakerManager;
    private final ThreadPoolTaskExecutor llmExecutor;

    /**
     * 流式调用（SSE 模式）
     */
    public CompletableFuture<Void> streamChat(
            String providerKey,
            LlmRequest request,
            LlmStreamCallback callback) {

        CircuitBreaker breaker = breakerManager.getBreaker(providerKey);

        return Decorators.ofSupplier(() ->
            rateLimiter.withRateLimit(providerKey, () ->
                executeStreamCall(providerKey, request, callback)
            )
        )
        .withCircuitBreaker(breaker)
        .decorate()
        .get();
    }

    private CompletableFuture<Void> executeStreamCall(
            String providerKey,
            LlmRequest request,
            LlmStreamCallback callback) {

        return CompletableFuture.runAsync(() -> {
            Request httpRequest = buildStreamRequest(providerKey, request);

            try (Response response = httpClient.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    throw new LlmException("HTTP " + response.code());
                }

                // 读取 SSE 流
                try (BufferedSource source = response.body().source()) {
                    while (!source.exhausted()) {
                        String line = source.readUtf8Line();
                        if (line == null) break;

                        // 解析 SSE 格式: data: {...}
                        if (line.startsWith("data: ")) {
                            String data = line.substring(6);
                            if ("[DONE]".equals(data)) {
                                callback.onComplete();
                                return;
                            }
                            String chunk = extractContent(data);
                            callback.onChunk(chunk);
                        }
                    }
                }
            } catch (IOException e) {
                callback.onError(e);
                throw new LlmException("Stream error", e);
            }
        }, llmExecutor);
    }

    private String extractContent(String jsonData) {
        // 解析 OpenAI/DeepSeek 等标准 SSE 格式
        // 提取 choices[0].delta.content
        return JsonUtil.extract(jsonData, "$.choices[0].delta.content");
    }
}

/**
 * LLM 流式回调
 */
public interface LlmStreamCallback {
    void onChunk(String chunk);
    void onComplete();
    void onError(Throwable error);
}
```

### 8.5 Tomcat 配置

```yaml
# application.yml
server:
  tomcat:
    threads:
      max: 200          # 最大工作线程（SSE 非阻塞，线程数可小于连接数）
      min-spare: 10
    max-connections: 8192   # 最大连接数
    accept-count: 100       # 等待队列
```

### 8.6 线程模型说明

```
浏览器 ──HTTP SSE──► Tomcat
                        │
                        ▼ NIO 非阻塞
                ┌───────────────┐
                │  SseEmitter   │ 持有连接，不占用线程
                └───────┬───────┘
                        │
                        ▼ 异步回调
                ┌───────────────┐
                │  llmExecutor  │ 统一线程池处理
                │  (CPU * 4)    │
                └───────┬───────┘
                        │
                        ▼ HTTP 调用
                   LLM Provider
```

**关键点**：
- 1000 个 SSE 连接 ≈ 10-20 个线程（Tomcat NIO）
- `emitter.send()` 非阻塞，发送完立即释放线程
- 实际业务逻辑（调用 LLM）在 `llmExecutor` 中执行

---

## 九、降级策略

### 8.1 Provider 级联降级

```
请求 OpenAI
    │
    ├── 成功 ──► 返回结果
    │
    ├── 熔断/超时/失败 ──► 降级到 DeepSeek
    │                           │
    │                           ├── 成功 ──► 返回结果
    │                           │
    │                           └── 失败 ──► 降级到 Ollama(本地)
    │                                               │
    │                                               ├── 成功 ──► 返回结果
    │                                               │
    │                                               └── 失败 ──► 返回错误
    │
    └── 限流 ──► 等待后重试，再失败则降级
```

### 8.2 降级配置

```java
private String getFallbackProvider(String primary) {
    return switch (primary) {
        case "openai" -> "deepseek";
        case "deepseek" -> "ollama";
        default -> null;  // 无可用降级
    };
}
```

---

## 十、监控指标

### 10.1 关键指标（Micrometer）

```yaml
# 请求指标
llm.request.duration:
  tags: [provider, model, status]
  type: histogram

llm.request.active:
  tags: [provider]
  type: gauge

# 熔断器指标
llm.circuitbreaker.state:
  tags: [provider, state]
  type: gauge

# 线程池指标
llm.executor.active:
  tags: [provider]
  type: gauge

llm.executor.queue.size:
  tags: [provider]
  type: gauge

# (Token 计费统计暂不实现)
```

### 10.2 线程池监控（统一池）

| 指标 | 类型 | 说明 |
|------|------|------|
| llm.executor.active | gauge | 全局活跃线程数 |
| llm.executor.queue.size | gauge | 全局队列积压数 |
| llm.executor.pool.size | gauge | 当前池大小 |

### 10.3 信号量监控（按 Provider）

| 指标 | 类型 | 说明 |
|------|------|------|
| llm.semaphore.active[provider] | gauge | Provider 当前占用信号量数 |
| llm.semaphore.waiting[provider] | gauge | Provider 等待信号量线程数 |

### 10.4 告警规则

| 指标 | 阈值 | 级别 | 动作 |
|------|------|------|------|
| 失败率 | > 30% | Warning | 通知 |
| 失败率 | > 50% | Critical | 熔断触发 |
| 队列积压 | > 50 | Warning | 扩容或限流 |
| P99 延迟 | > 10s | Warning | 通知 |
| 熔断器开启 | - | Info | 自动降级 |

---

## 十一、配置示例

```yaml
hify:
  llm:
    providers:
      openai:
        base-url: https://api.openai.com/v1
        api-key: ${OPENAI_API_KEY}
        max-connections: 50
        max-concurrent-requests: 20
        connect-timeout: 5s
        read-timeout: 60s
        total-timeout: 90s

      deepseek:
        base-url: https://api.deepseek.com/v1
        api-key: ${DEEPSEEK_API_KEY}
        max-connections: 30
        max-concurrent-requests: 10  # 限流更严格
        connect-timeout: 5s
        read-timeout: 60s
        total-timeout: 90s

      ollama:
        base-url: http://localhost:11434
        max-connections: 10
        max-concurrent-requests: 5
        connect-timeout: 2s
        read-timeout: 300s   # 本地模型慢
        total-timeout: 600s
```

---

## 十二、关键设计决策

| 问题 | 方案 | 理由 |
|------|------|------|
| **线程池设计** | **统一线程池 + Provider 级信号量** | 用户可配 N 个 Provider，线程数必须可控 |
| 线程池大小 | 核心：CPU * 2，最大：CPU * 4 | 平衡并发能力与资源占用 |
| Provider 限流 | 信号量（内存级，动态创建） | 比线程池轻量，每个 Provider 仅一个 Semaphore |
| 连接池设计 | 全局共享，按 Host 限制 | 减少连接数，提高复用率 |
| 熔断粒度 | 按 Provider | 局部故障不影响全局 |
| 重试退避 | 指数退避 + 最大延迟 | 避免雪崩 |
| 流式超时 | 数据级空闲检测 | 防止长连接假死 |
| 降级策略 | Provider 级联 | OpenAI → DeepSeek → Ollama |

### 资源占用对比

| 场景 | 原方案 | 新方案 |
|------|--------|--------|
| 5 个 Provider | 5 个线程池（最多 250 线程） | 1 个线程池（最多 32 线程） |
| 50 个 Provider | 50 个线程池（失控） | 1 个线程池（最多 32 线程） |
| 内存占用（信号量 vs 线程池） | 高（每个线程 ~1MB） | 低（Semaphore ~几十字节）|

---

## 十三、已确认决策

| 决策 | 结论 | 说明 |
|------|------|------|
| 流式协议 | **SSE (SseEmitter)** | Spring MVC 原生支持，无需 WebFlux |
| Token 计费 | **不做** | 内部使用，暂不统计成本 |
| 模型预热 | **不做** | 运行时切换不预热，直接切换 |
| 结果缓存 | **不做** | 相同 Prompt 不缓存，每次都请求 |

---

*文档完*
