# CLAUDE.md

本文档为 Claude Code (claude.ai/code) 提供本代码仓库的工作指南。

## 项目概述

**Hify** 是一个面向团队内部的 AI Agent 开发平台，简化版 Dify，支持可视化工作流编排、RAG 知识库和 MCP 工具扩展。

### 产品定位

- **目标用户**：20-50 人技术团队内部使用
- **部署方式**：本地 Docker Compose 一键部署
- **核心场景**：内部知识库问答、自动化工作流、AI Agent 开发

### 做什么

- **多模型提供商管理**：两级架构（提供商 → 模型），支持 OpenAI、DeepSeek、通义千问等
- **Agent 创建与配置**：选模型、绑工具、设系统提示词、配置参数
- **对话引擎**：流式响应（SSE）、多轮对话、上下文管理
- **知识库 + RAG**：支持 TXT/Markdown 文档（Markdown 清洗为纯文本），固定长度分块，向量检索
- **简版工作流**：JSON 配置，线性执行 + 条件分支，不做可视化拖拽
- **MCP 工具接入**：Agent 可通过 MCP 协议调用外部工具，动态工具发现
- **管理控制台**：模型管理、Agent 配置、对话界面、
### 不做什么

- 不做人机协同（HITL）、A/B 测试、复杂 RAG 分块策略
- 不做 50+ 内置工具（5 个核心 + MCP 扩展）
- 不做企业级 SSO、审计日志、多租户隔离
- 不做 BaaS 对外开放 API（纯 Web 界面）

### 技术栈

| 层级 | 技术 |
|------|------|
| 前端 | Vue 3 + TypeScript + Element Plus |
| 后端 | Spring Boot 3.x + MyBatis-Plus |
| 数据库 | PostgreSQL 15+（业务数据 + pgvector 向量扩展）|
| 缓存 | Redis 7（会话、SSE 流式、热点数据）|
| 部署 | K8s（生产）/ Docker Compose（开发）|

### 部署架构（K8s）

```
Ingress (Nginx) → hify-app Pod (Nginx + Spring Boot) × 2-3 副本
                        ↓
    ┌──────────────┬──────────────┬──────────────┐
PostgreSQL    Redis       外部 LLM API
(StatefulSet) (内存+持久化)  (互联网出口)
```

| 组件 | 类型 | 资源限制 | 说明 |
|------|------|---------|------|
| hify-app | Deployment | CPU: 500m-1000m, Mem: 1Gi-2Gi | 前后端同 Pod，2-3 副本，HPA |
| PostgreSQL | StatefulSet | CPU: 500m, Mem: 1Gi, 磁盘: 10Gi | 业务数据 + pgvector |
| Redis | StatefulSet | CPU: 200m, Mem: 512Mi | Session + SSE Pub/Sub |
| Ingress | Ingress | - | SSL + SSE 长连接支持 |

### 部署与运维预期

- **单机部署**：一期 20-50 人同时在线，峰值 3-5 QPS
- **资源限制**：JVM `-Xmx512m`，容器内存 768M，PostgreSQL 512M，Redis 384M
- **监控起步**：Spring Boot Actuator + 日志，预留 Prometheus + Grafana
- **扩容预留**：无状态设计，后期支持水平扩容

## 开发阶段（10 周）

| 阶段 | 周期 | 内容 | 产出 |
|------|------|------|------|
| 1 | 2 周 | 基础架构 | Vue + Spring Boot 框架，登录模块 |
| 2 | 3 周 | Agent 核心 | 模型网关（两级架构），ReAct Agent，5 个内置工具 |
| 3 | 2 周 | MCP 集成 | SSE 传输，工具发现，上下文管理 |
| 4 | 2 周 | 工作流 + RAG | 6 节点画布，pgvector 检索 |
| 5 | 1 周 | Polish + 部署 | 流式优化，Docker Compose |

**关键简化决策**：
- MCP 只做客户端（调用外部工具），不做服务端
- 工作流不做可视化拖拽，JSON 配置即可
- RAG 只用固定长度分块，不做父子/Q&A 提取

## 代码组织

多模块单体架构，预留后期拆分为微服务的能力。

### 模块结构

```
hify/
├── hify-common/
│   ├── hify-common-core/  # 纯工具，无 Spring
│   └── hify-common-web/   # Web 公共组件
├── hify-module-model/     # 模型提供商管理
├── hify-module-agent/     # Agent 配置
├── hify-module-chat/      # 对话引擎
├── hify-module-rag/       # 知识库 RAG
├── hify-module-workflow/  # 工作流
├── hify-module-mcp/       # MCP 工具
└── hify-server/           # 启动器（组装所有模块）
```

### 模块内分层（强制）

```
com.hify.xxx/
├── api/         # 对外暴露的唯一入口，其他模块只能调用这层
├── service/     # 业务逻辑实现
├── mapper/      # MyBatis 数据访问
├── entity/      # Entity（数据库映射）
├── dto/         # 请求 DTO（Controller 入参）
├── vo/          # 响应 VO（Controller 出参 / 层间传输）
├── controller/  # HTTP 接口（可选）
└── config/      # 模块配置（可选）
```

### 各层职责

| 层级 | 职责 | 禁止 |
|------|------|------|
| **api/** | 定义接口和 DTO，供其他模块调用 | 返回 Entity、使用内部类 |
| **service/** | 业务逻辑，实现 api 接口 | 直接调用其他模块 Service/Mapper |
| **mapper/** | SQL 映射，仅限本模块使用 | 在其他模块中引用 |
| **entity/** | 数据库映射 Entity | 包含业务逻辑、跨模块共享 |
| **dto/** | 请求 DTO（Controller 入参） | 返回给前端以外的层 |
| **vo/** | 响应 VO（Controller 出参 / 层间传输） | 包含业务逻辑、跨模块共享 |
| **controller/** | 接收参数、入参校验、权限检查、调用并返回 | 写业务逻辑、直接返回 Entity |

### 跨模块调用规则

- **只能通过 api/**：禁止注入其他模块的 Service/Mapper
- **循环依赖禁止**：使用 Spring 事件解耦
- **数据隔离**：各模块表前缀独立（`model_`、`agent_`、`chat_` 等）

### 示例：chat 模块调用 model 模块

```java
// ✅ 正确：注入 api 接口
@Service
public class ChatService {
    @Autowired
    private ModelProviderApi modelProviderApi;

    public void chat(Long modelId) {
        ModelDTO model = modelProviderApi.getModelById(modelId);
        // ...
    }
}

// ❌ 错误：直接注入其他模块的 Service
@Autowired
private ModelProviderService modelProviderService;
```

### 代码审查 Checklist

- [ ] **跨模块调用**：必须通过 `api/` 层，禁止直接注入 Service/Mapper
- [ ] **返回值**：api/ 层返回 DTO，禁止返回 Entity
- [ ] **SQL 位置**：复杂查询必须写 XML，禁止 `@Select` 注解 SQL
- [ ] **事务**：Service 方法加 `@Transactional`，读操作加 `readOnly = true`
- [ ] **循环依赖**：出现循环依赖用 Spring 事件解耦，禁止双向注入

## 关键技术决策

### LLM API 调用

| 问题 | 决策 | 说明 |
|------|------|------|
| 线程管理 | 统一线程池 + Provider 信号量限流 | 核心：CPU * 2，最大：CPU * 4，队列：500 |
| HTTP 连接池 | 全局共享，按 Host 限流 | 最大 200 连接，单 Host 50 |
| 超时策略 | 三层：连接 5s / 读取 60s / 流式空闲 30s | 防止假死 |
| 重试 | 指数退避（1s→10s），Provider 级联降级 | OpenAI → DeepSeek → Ollama |
| 熔断 | 按 Provider，失败率 50% / 30s 恢复 | Resilience4j |
| 流式协议 | SSE + SseEmitter | Spring MVC 原生，不引入 WebFlux |

## 流式响应设计（SSE）

### 协议选择

| 方案 | 选择 | 理由 |
|------|------|------|
| SSE | ✅ | 单向服务器推送、基于 HTTP、自动重连、浏览器原生支持 |
| WebSocket | ❌ | 双向通信、协议升级复杂、需处理心跳、Spring MVC 支持弱 |

### 实现架构

```
浏览器 ←──SSE──→ Ingress (长连接) ←──→ SseEmitter (Tomcat)
                                            ↓
                                    Redis Pub/Sub（流式数据缓冲）
                                            ↓
                                    LLM API SSE 流
```

### 连接管理

| 配置项 | 值 | 说明 |
|--------|-----|------|
| SseEmitter 超时 | `0`（不超时） | 业务层控制，避免连接中断 |
| 流式空闲超时 | 30s | 每 30s 必须收到数据，否则断开 |
| Tomcat read-timeout | 600s | Ingress 层保持长连接 |
| 前端重连 | EventSource.onerror | 自动重连，携带 last_id 断点续传 |

### 消息格式

```json
// 标准消息
event: message
id: 550e8400-e29b-41d4-a716-446655440000
data: {"content": "你好", "timestamp": 1704067200000}

// 结束标记
event: done
data: {"status": "completed"}

// 错误消息
event: error
data: {"code": "LLM_TIMEOUT", "message": "响应超时"}
```

### 后端关键代码

```java
@GetMapping(value = "/stream/{sessionId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter streamChat(@PathVariable String sessionId, @RequestParam String message) {
    SseEmitter emitter = new SseEmitter(0L); // 0 = 不超时

    chatService.streamChat(sessionId, message, chunk -> {
        emitter.send(SseEmitter.event()
            .id(UUID.randomUUID().toString())
            .name("message")
            .data(Map.of("content", chunk, "timestamp", System.currentTimeMillis())));
    });

    // 生命周期管理
    emitter.onCompletion(() -> chatService.stopStream(sessionId));
    emitter.onTimeout(() -> chatService.stopStream(sessionId));
    emitter.onError((e) -> chatService.stopStream(sessionId));

    return emitter;
}
```

### 前端接收示例

```javascript
const eventSource = new EventSource(`/api/chat/stream/${sessionId}?message=${encodeURIComponent(message)}`);

// 接收消息
eventSource.addEventListener('message', (e) => {
    const data = JSON.parse(e.data);
    appendMessage(data.content);
});

// 完成关闭
eventSource.addEventListener('done', (e) => {
    eventSource.close();
});

// 错误处理（自动重连）
eventSource.onerror = (e) => {
    if (eventSource.readyState === EventSource.CLOSED) {
        // 连接已关闭，无需重连
        return;
    }
    // 网络错误，浏览器会自动重连
    console.log('SSE 连接中断，正在重连...');
};
```

### 关键注意事项

1. **Nginx 配置**：必须关闭 `proxy-buffering` 和 `proxy-cache`，否则 SSE 被缓冲
2. **连接数**：Tomcat NIO 模式下，1000 个 SSE 连接 ≈ 10-20 个线程
3. **断点续传**：前端记录最后收到的 `id`，重连时通过 `Last-Event-ID` header 传递
4. **异常处理**：LLM 报错通过 `event: error` 发送，前端区分业务错误和连接错误

### 监控指标（P0 告警）

```yaml
# 必须立即处理
llm_request_duration_seconds{quantile="0.99"} > 30    # LLM 响应过慢
llm_circuitbreaker_state == 1                          # 熔断器开启
jvm_memory_used_bytes / jvm_memory_max_bytes > 0.9     # JVM 内存不足
container_memory_working_set_bytes > 700Mi             # 容器内存不足

# 关注趋势
llm_semaphore_waiting > 5      # Provider 限流排队
tomcat_connections_active > 800 # 连接数过高
postgresql_connections_active > 8 # 数据库连接过多
```

### 数据存储

| 数据 | 存储 | 说明 |
|------|------|------|
| 业务数据 | PostgreSQL | 用户、会话、配置 |
| 向量检索 | pgvector 扩展 | 同一实例，简化部署 |
| 缓存/会话 | Redis | SSE 流式、热点数据 |

---

## 扩展规范

生成代码时需同时遵循以下文档：

| 文档 | 内容 | 何时使用 |
|------|------|----------|
| [docs/database-guidelines.md](docs/database-guidelines.md) | PostgreSQL 建表规范、字段类型、索引、分页 | 编写实体类、Mapper、建表 SQL |
| [docs/java-coding-standards.md](docs/java-coding-standards.md) | 阿里巴巴编码规范（命名、异常、日志、并发） | 编写所有 Java 代码 |

## 开发环境

- **JDK**: JDK 17 (ms-17)
- **语言级别**: Java 17
- **构建工具**: Maven
- **输出目录**: `out/`

---

## 本地开发部署

### 方式一：Docker Compose（推荐）

项目已配置 Docker Compose 一键启动 PostgreSQL + Redis：

```bash
# 启动数据库（确保 Docker 运行中）
docker compose up -d

# 查看状态
docker compose ps

# 查看日志
docker compose logs -f

# 停止
docker compose down

# 彻底清理（包括数据卷，谨慎使用）
docker compose down -v
```

**数据挂载说明：**
| 服务 | 容器路径 | 本地卷名 | 说明 |
|------|---------|---------|------|
| PostgreSQL | `/var/lib/postgresql/data` | `postgres_data` | 业务数据 + pgvector |
| Redis | `/data` | `redis_data` | 会话 + 缓存 |

**数据库初始化：**
- 首次启动自动执行 `docs/sql/init/*.sql`
- 包含：pgvector 扩展、通用触发器函数

### 方式二：本地 H2 内存数据库（快速测试）

无需 Docker，使用 H2 内存数据库 + 本地 Redis：

```bash
# 启动 Redis（如果有）
docker run -d --name hify-redis -p 6379:6379 redis:latest

# 启动应用（使用 local 配置）
cd hify-server
mvn spring-boot:run -Dspring-boot.run.profiles=local

# 或运行 jar
java -jar target/hify-server-*.jar --spring.profiles.active=local
```

**H2 控制台：** http://localhost:8080/api/h2-console
- JDBC URL: `jdbc:h2:mem:hify`
- 用户名: `sa`
- 密码: （留空）

### 启动应用

```bash
# 1. 编译
cd /Users/linjiongxin/IdeaProjects/my-hify
mvn clean install -DskipTests

# 2. 启动（开发环境使用 local 配置）
cd hify-server
mvn spring-boot:run -Dspring-boot.run.profiles=local

# 3. 验证
open http://localhost:8080/api/actuator/health
```

