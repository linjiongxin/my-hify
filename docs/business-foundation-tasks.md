# Hify 业务开发前基础组件清单

> 本文档梳理 Hify 从工程骨架进入业务开发前，仍需补齐的基础组件。
> 每项均说明：**解决什么问题**、**建议落位模块**、**核心产出物**、**验收标准**。

---

## 一、P0 最高优先级（本周必须完成）

### 任务 1：搭建 JWT 认证体系

| 项目 | 说明 |
|------|------|
| **解决什么问题** | 管理后台的登录鉴权；模型配置、Agent 配置等敏感接口不能裸奔 |
| **建议模块** | `hify-common-web`（公共认证组件）+ `hify-server`（登录 Controller） |
| **核心产出物** | JWT Token 生成/解析/校验工具类、登录接口、Security 拦截器、请求上下文（CurrentUser） |
| **验收标准** | 未携带 Token 访问管理接口返回 401；携带有效 Token 可正常访问并获取当前用户 |

---

### 任务 2：实现 TraceID 全链路追踪（MDC）

| 项目 | 说明 |
|------|------|
| **解决什么问题** | `logback-spring.xml` 中已有 `%X{X-Request-ID}` 占位符，但**没有任何代码往 MDC 里放值**，链路追踪目前是个空壳 |
| **建议模块** | `hify-common-web`（Servlet Filter） |
| **核心产出物** | `TraceIdFilter`：从请求头读取 `X-Request-ID`（不存在则生成 UUID），写入 MDC，并在响应头返回 |
| **验收标准** | 任意接口请求后，日志中均包含 `[X-Request-ID]` 且响应头携带相同值 |

---

### 任务 3：构建 LLM Provider 统一网关

| 项目 | 说明 |
|------|------|
| **解决什么问题** | 屏蔽 OpenAI / DeepSeek / 通义千问等协议差异；chat、agent、workflow 都会调用 LLM，不能各自写 HTTP 客户端 |
| **建议模块** | `hify-module-model`（作为 api/ 层的一部分对外暴露） |
| **核心产出物** | Provider 配置实体、统一请求/响应 DTO、LLM Client 接口与默认实现、按 Provider 的信号量限流、超时/重试/降级预留 |
| **验收标准** | 新增一个 Provider 只需实现统一接口并注册配置，chat/agent 模块调用方式不变 |

---

### 任务 4：集成 SpringDoc / Knife4j API 文档

| 项目 | 说明 |
|------|------|
| **解决什么问题** | Vue 前端和 Spring Boot 后端并行开发，必须有在线文档作为接口对齐基准 |
| **建议模块** | `hify-server`（配置入口）+ `hify-common-web`（通用响应包装说明） |
| **核心产出物** | OpenAPI 分组配置、认证 Token 注入方式、通用 `Result<T>` 的泛型文档说明 |
| **验收标准** | 访问 `/api/doc.html` 能看到所有 Controller 接口，支持在线调试和 Token 填入 |

---

### 任务 5：启用 @EnableAsync 并封装接口幂等性

| 项目 | 说明 |
|------|------|
| **解决什么问题** | 已有 `ThreadPoolConfig` 三个线程池，但**没有开启异步注解支持**，`@Async` 当前不生效；LLM 调用贵，前端重试不能导致重复执行 |
| **建议模块** | `hify-common-web`（注解 + AOP 拦截器） |
| **核心产出物** | `@EnableAsync` 配置、`@Idempotent` 注解 + 拦截器（基于 `Idempotency-Key` + Redis） |
| **验收标准** | `@Async` 方法能正确投递到对应线程池；携带相同 `Idempotency-Key` 的并发请求只执行一次 |

---

## 二、P1 高优先级（阶段 2 开始前完成）

### 任务 6：补齐 Prometheus 指标与自定义健康检查

| 项目 | 说明 |
|------|------|
| **解决什么问题** | Actuator 已暴露 `/prometheus` endpoint，但 POM 中**缺少指标导出依赖**；K8s `readinessProbe` 需要感知关键依赖可用性 |
| **建议模块** | `hify-server`（依赖引入 + 配置）+ 各模块（自定义 HealthIndicator） |
| **核心产出物** | `micrometer-registry-prometheus` 依赖、PostgreSQL / Redis / LLM Provider 的 `HealthIndicator`、业务指标埋点工具类 |
| **验收标准** | Prometheus 能抓取到 JVM 和应用指标；`/actuator/health` 能正确反映 DB/Redis/LLM 状态 |

---

### 任务 7：封装缓存抽象与 Redisson 分布式锁

| 项目 | 说明 |
|------|------|
| **解决什么问题** | 模型列表、Agent 配置读多写少，不能每次都查 DB；工作流调度、RAG 文档分块需要分布式互斥 |
| **建议模块** | `hify-common-web`（缓存配置 + Redisson 封装） |
| **核心产出物** | `@EnableCaching` + Caffeine + Redis 二级缓存配置、`DistributedLock` 工具类（含可重入、自动续期） |
| **验收标准** | 热点数据命中本地缓存；分布式锁在并发场景下无重复执行 |

---

## 三、P2 中优先级（阶段 3 前完成）

### 任务 8：抽象文件存储接口

| 项目 | 说明 |
|------|------|
| **解决什么问题** | RAG 知识库需要上传 TXT/Markdown，提前抽象存储接口，避免后期硬编码本地路径或特定云厂商 SDK |
| **建议模块** | `hify-common-core`（SPI 接口）+ `hify-common-web`（本地默认实现） |
| **核心产出物** | `FileStorageService` 接口（upload / download / delete / getUrl）、本地文件系统默认实现、配置属性类 |
| **验收标准** | 切换存储介质只需替换实现类并改配置，业务代码零改动 |

---

### 任务 9：建立 Spring Event 事件总线规范

| 项目 | 说明 |
|------|------|
| **解决什么问题** | CLAUDE.md 强制要求禁止循环依赖，用 Spring 事件解耦，但项目里目前没有任何事件相关代码 |
| **建议模块** | `hify-common-web`（事件基类 + 发布器）+ 任意模块（示例事件） |
| **核心产出物** | `BaseApplicationEvent` 基类、`EventPublisher` 封装、一个跨模块解耦示例（如 Agent 创建成功事件） |
| **验收标准** | 后续跨模块解耦必须通过事件总线，代码审查有明确 Checklist 可依 |

---

## 四、建议落地顺序

```
Week 1（当前）
  ├── 任务 1：JWT 认证体系
  ├── 任务 2：TraceID 全链路追踪
  ├── 任务 4：SpringDoc / Knife4j
  └── 任务 5：@EnableAsync + 接口幂等性

Week 2（阶段 2 启动前）
  ├── 任务 3：LLM Provider 统一网关
  ├── 任务 6：Prometheus + 健康检查
  └── 任务 7：缓存抽象 + Redisson 分布式锁

Week 4~5（阶段 3 启动前）
  ├── 任务 8：文件存储抽象
  └── 任务 9：Spring Event 事件总线规范
```

---

## 五、已知已就绪（无需重复建设）

- ✅ Maven 多模块骨架
- ✅ `hify-common`：Result 封装、BizException、全局异常处理、分页参数/结果
- ✅ MyBatis-Plus 配置 + BaseEntity（ASSIGN_ID、自动填充、逻辑删除）
- ✅ Redis 配置（Jackson 序列化）
- ✅ 线程池配置（common / llm / sse 三个线程池）
- ✅ CORS 配置（含 SSE 所需响应头暴露）
- ✅ Actuator + logback 滚动日志 + Prometheus endpoint 暴露配置
- ✅ PostgreSQL + pgvector 扩展 + H2 本地开发配置
- ✅ Docker Compose（PostgreSQL + Redis）

---

*文档生成时间：2026-04-14*
