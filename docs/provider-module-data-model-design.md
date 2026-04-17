# Provider 模块数据模型设计方案

> 设计目标：支撑 Hify 平台对 90% 以上 LLM 厂商的兼容管理，兼顾配置驱动、鉴权差异统一存储、健康状态隔离、以及未来扩展性。
>
> 定稿时间：2026-04-17

---

## 一、设计背景与约束

### 1.1 现状问题

原有 demo 实现中：

- `OpenAiCompatibleProvider` 仅支持标准 OpenAI 协议，认证方式写死为 `Authorization: Bearer {apiKey}`
- 厂商和配置混在一起，新增一家兼容厂商需要改 Java 代码
- 健康状态没有明确的存储位置
- 模型能力与模型元信息混在同一层，无法做能力过滤

### 1.2 核心诉求

1. **配置驱动**：新增一个 OpenAI 兼容厂商，只需插数据库记录，不写 Java 代码
2. **鉴权统一**：Bearer、API Key、无认证、自定义 Header 等多种鉴权方式能用同一套结构存储
3. **状态隔离**：健康状态变化频繁，不应和稳定的厂商元信息绑在一起
4. **模型管理**：一个供应商下多个模型，每个模型有独立的能力参数

### 1.3 约束条件

- 目标用户为 20-50 人技术团队，厂商数量通常不超过 20 家
- 数据库为 PostgreSQL，支持 JSONB 和 GIN 索引
- 模块分层严格（`api/service/mapper/entity`），表结构应尽量减少 JOIN 复杂度

---

## 二、设计演进过程

### 第一阶段：四表方案（过度设计）

最初考虑过拆成：

- `model_provider`：厂商元信息
- `model_provider_config`：运行时配置/API Key
- `model`：模型定义
- `model_provider_status`：健康状态历史

**被淘汰原因**：
- 对于内部平台，一个厂商通常只配一个 API Key，`config` 表的一对多能力几乎用不上
- 厂商数量极少，健康状态拆成独立 log 表会增加无意义的 JOIN 和清理逻辑
- 四张表导致数据层过于复杂，维护成本高

### 第二阶段：两表方案（极简）

将配置和状态全部合并到 `model_provider` 中：

- `model_provider`：元信息 + 鉴权 + 健康状态
- `model_config`：模型定义

**优势**：查询直接、无多余 JOIN、代码最简单。
**劣势**：健康状态更新会锁定 `model_provider` 行，若管理后台同时编辑厂商信息可能产生锁竞争；虽然概率极低，但从职责分离角度不够干净。

### 第三阶段：三表方案（最终定稿）

折中后确定为：

- **`model_provider`**：稳定元信息 + 鉴权配置（运行时只读为主）
- **`model_provider_status`**：易变健康状态（高频更新，完全隔离）
- **`model_config`**：模型能力与参数（配置层）

**选择理由**：
- `model_provider` 里直接存 `api_key` 和 `auth_config`，省掉了无意义的 `config` 拆表
- `status` 单独成表，职责边界清晰，也方便未来扩展 QPS、错误统计等运行时指标
- 总共 3 张表，JOIN 最多两层，对 PostgreSQL 完全无压力

---

## 三、表结构设计

### 3.1 model_provider（厂商定义与鉴权配置）

```sql
CREATE TABLE model_provider (
    id              BIGINT PRIMARY KEY,
    name            VARCHAR(64) NOT NULL,
    code            VARCHAR(32) NOT NULL,

    -- 协议层：决定后端用哪个 LlmProvider 实现处理
    protocol_type   VARCHAR(32) DEFAULT 'openai_compatible',

    -- 默认 API 基础地址
    api_base_url    VARCHAR(256) NOT NULL,

    -- 鉴权配置（直接内联，无需单独 config 表）
    auth_type       VARCHAR(32) DEFAULT 'BEARER',   -- BEARER | API_KEY | NONE | CUSTOM
    api_key         VARCHAR(512),                   -- 主密钥
    auth_config     JSONB DEFAULT '{}',             -- 结构化额外鉴权参数

    -- 控制字段
    enabled         BOOLEAN DEFAULT TRUE,
    sort_order      INT DEFAULT 0,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE UNIQUE INDEX uk_model_provider_code ON model_provider(code) WHERE deleted = FALSE;
```

#### auth_config JSONB 的 Schema 约定

不同 `auth_type` 对应不同结构，后端用统一 POJO 反序列化，无需动态拆表：

| auth_type | 示例 | 适用厂商 |
|-----------|------|----------|
| **BEARER** | `{"apiKey": "sk-xxx"}` | OpenAI, DeepSeek, Qwen, Kimi |
| **API_KEY** | `{"apiKey": "xxx", "headerName": "api-key", "prefix": ""}` | Azure OpenAI |
| **NONE** | `{}` | Ollama, 部分内网 vLLM |
| **CUSTOM** | `{"headers": {"X-Auth": "xxx"}}` | 私有网关、定制部署 |

**为什么用 JSONB 而不是拆表**：
- 鉴权差异是"同一件事的不同参数组合"，不是不同的业务实体
- 新增鉴权方式无需 DDL 变更，直接新增一个 `auth_type` 标签即可
- PostgreSQL JSONB 支持索引和高效查询，性能完全满足需求

---

### 3.2 model_provider_status（运行时健康状态）

```sql
CREATE TABLE model_provider_status (
    provider_id         BIGINT PRIMARY KEY REFERENCES model_provider(id) ON DELETE CASCADE,

    -- 健康检查结果
    health_status       VARCHAR(16) DEFAULT 'unknown',  -- healthy | degraded | unhealthy | unknown
    health_checked_at   TIMESTAMP,
    health_latency_ms   INTEGER,
    health_error_msg    TEXT,

    -- 运行统计（可选扩展）
    total_requests      BIGINT DEFAULT 0,
    failed_requests     BIGINT DEFAULT 0,
    last_error_at       TIMESTAMP,
    last_error_code     VARCHAR(64),

    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_provider_status_health ON model_provider_status(health_status);
```

**设计要点**：
- 与 `model_provider` 严格 1:1，通过 `provider_id` 外级联删除
- 健康检查任务只更新此表，不影响元信息表的稳定数据
- 预留 `total_requests`、`failed_requests` 等统计字段，供后续做可用性趋势分析

**health_status 语义**：

| 状态 | 含义 | 用户侧行为 |
|------|------|-----------|
| `healthy` | 最近检查通过 | 正常展示，推荐使用 |
| `degraded` | 偶发超时或慢响应 | 可用，但提示"响应较慢" |
| `unhealthy` | 连续检查失败 | 置灰，禁止新建 Agent/会话 |
| `unknown` | 从未检查或刚添加 | 允许使用，提示"未验证" |

---

### 3.3 model_config（模型定义与能力参数）

```sql
CREATE TABLE model_config (
    id              BIGINT PRIMARY KEY,
    provider_id     BIGINT NOT NULL REFERENCES model_provider(id) ON DELETE CASCADE,

    -- 基本信息
    name            VARCHAR(64) NOT NULL,           -- 展示名：GPT-4o
    model_id        VARCHAR(64) NOT NULL,           -- API 调用名：gpt-4o

    -- 能力参数
    max_tokens      INT DEFAULT 4096,               -- 最大生成 token 数
    context_window  INT DEFAULT 8192,               -- 上下文窗口长度

    -- 能力矩阵（JSONB，支持前端过滤与后端调用前校验）
    capabilities    JSONB DEFAULT '{
        "chat": true,
        "streaming": true,
        "vision": false,
        "toolCalling": false,
        "reasoning": false,
        "jsonMode": false
    }',

    -- 成本参考（可选）
    input_price_per_1m  DECIMAL(10,6),
    output_price_per_1m DECIMAL(10,6),

    -- 控制字段
    is_default      BOOLEAN DEFAULT FALSE,          -- 该 provider 下的默认模型
    enabled         BOOLEAN DEFAULT TRUE,
    sort_order      INT DEFAULT 0,

    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE UNIQUE INDEX uk_model_config_model_id ON model_config(model_id) WHERE deleted = FALSE;
CREATE INDEX idx_model_config_provider_id ON model_config(provider_id);
CREATE INDEX idx_model_config_capabilities ON model_config USING GIN (capabilities);
```

#### capabilities 标准 Schema

```json
{
    "chat": true,           // 支持基础对话
    "streaming": true,      // 支持 SSE 流式输出
    "vision": false,        // 支持图片/多模态输入
    "toolCalling": true,    // 支持函数/工具调用
    "reasoning": false,     // 支持深度思考（如 o1, DeepSeek-R1）
    "jsonMode": true        // 支持强制 JSON/结构化输出
}
```

**设计要点**：
- `model_id` 全局唯一，后端调用 LLM API 时直接使用
- `capabilities` 用 GIN 索引加速查询（如"找出所有支持 toolCalling 的模型"）
- `is_default` 用于简化用户创建 Agent 时的模型选择体验

---

## 四、初始化数据示例

```sql
-- OpenAI
INSERT INTO model_provider (id, name, code, protocol_type, api_base_url, auth_type, api_key, auth_config, enabled, sort_order)
VALUES (1, 'OpenAI', 'openai', 'openai_compatible', 'https://api.openai.com/v1', 'BEARER', '', '{}', true, 1);

INSERT INTO model_config (id, provider_id, name, model_id, max_tokens, context_window, capabilities, is_default, enabled)
VALUES
(1, 1, 'GPT-4o', 'gpt-4o', 4096, 128000, '{"chat":true,"streaming":true,"vision":true,"toolCalling":true,"reasoning":false,"jsonMode":true}', true, true),
(2, 1, 'GPT-4o Mini', 'gpt-4o-mini', 4096, 128000, '{"chat":true,"streaming":true,"vision":true,"toolCalling":true,"reasoning":false,"jsonMode":true}', false, true),
(3, 1, 'o3 Mini', 'o3-mini', 4096, 200000, '{"chat":true,"streaming":true,"vision":false,"toolCalling":true,"reasoning":true,"jsonMode":true}', false, true);

-- DeepSeek
INSERT INTO model_provider (id, name, code, protocol_type, api_base_url, auth_type, api_key, auth_config, enabled, sort_order)
VALUES (2, 'DeepSeek', 'deepseek', 'openai_compatible', 'https://api.deepseek.com/v1', 'BEARER', '', '{}', true, 2);

INSERT INTO model_config (id, provider_id, name, model_id, max_tokens, context_window, capabilities, is_default, enabled)
VALUES
(4, 2, 'DeepSeek V3', 'deepseek-chat', 8192, 64000, '{"chat":true,"streaming":true,"vision":false,"toolCalling":true,"reasoning":false,"jsonMode":true}', true, true),
(5, 2, 'DeepSeek R1', 'deepseek-reasoner', 8192, 64000, '{"chat":true,"streaming":true,"vision":false,"toolCalling":true,"reasoning":true,"jsonMode":true}', false, true);

-- Ollama（本地，无认证）
INSERT INTO model_provider (id, name, code, protocol_type, api_base_url, auth_type, api_key, auth_config, enabled, sort_order)
VALUES (3, 'Ollama', 'ollama', 'openai_compatible', 'http://localhost:11434/v1', 'NONE', '', '{}', true, 99);

INSERT INTO model_config (id, provider_id, name, model_id, max_tokens, context_window, capabilities, is_default, enabled)
VALUES (6, 3, 'Llama 3.1', 'llama3.1', 4096, 128000, '{"chat":true,"streaming":true,"vision":false,"toolCalling":false,"reasoning":false,"jsonMode":false}', true, true);

-- 通义千问
INSERT INTO model_provider (id, name, code, protocol_type, api_base_url, auth_type, api_key, auth_config, enabled, sort_order)
VALUES (4, '通义千问', 'qwen', 'openai_compatible', 'https://dashscope.aliyuncs.com/compatible-mode/v1', 'BEARER', '', '{}', true, 3);

INSERT INTO model_config (id, provider_id, name, model_id, max_tokens, context_window, capabilities, is_default, enabled)
VALUES
(7, 4, 'Qwen2.5-72B', 'qwen2.5-72b-instruct', 8192, 131072, '{"chat":true,"streaming":true,"vision":false,"toolCalling":true,"reasoning":false,"jsonMode":true}', true, true),
(8, 4, 'Qwen-VL-Max', 'qwen-vl-max', 2048, 32768, '{"chat":true,"streaming":true,"vision":true,"toolCalling":true,"reasoning":false,"jsonMode":true}', false, true);

-- Azure OpenAI（API_KEY 鉴权）
INSERT INTO model_provider (id, name, code, protocol_type, api_base_url, auth_type, api_key, auth_config, enabled, sort_order)
VALUES (5, 'Azure OpenAI', 'azure_openai', 'openai_compatible', 'https://your-resource.openai.azure.com/openai/deployments', 'API_KEY', '', '{"headerName":"api-key","prefix":""}', true, 4);

INSERT INTO model_config (id, provider_id, name, model_id, max_tokens, context_window, capabilities, is_default, enabled)
VALUES (9, 5, 'GPT-4o (Azure)', 'gpt-4o', 4096, 128000, '{"chat":true,"streaming":true,"vision":true,"toolCalling":true,"reasoning":false,"jsonMode":true}', true, true);
```

---

## 五、关键设计决策总结

| 问题 | 方案 | 理由 |
|------|------|------|
| 多种鉴权差异怎么统一存储 | `auth_type` 标签 + `auth_config` JSONB | 结构灵活，新增鉴权方式不改表结构，统一 POJO 反序列化 |
| 一个供应商下多个模型怎么管理 | `model_config.provider_id` 外键 + `capabilities` JSONB | 清晰一对多，能力矩阵支持前端筛选与后端校验 |
| 供应商健康状态怎么表示 | 独立 `model_provider_status` 表 | 高频更新与稳定元信息隔离，避免锁竞争，方便扩展运行时统计 |
| 为什么不保留 model_provider_config 表 | 内部平台一个厂商通常只有一个配置 | 多拆一张表就多一个 JOIN 和维护成本，直接合并到 provider 更简洁 |
| 新增兼容厂商需要做什么 | 插 2-3 条 SQL（1 条 provider + 1-N 条 model_config） | 不需要写任何 Java 代码，真正做到配置驱动 |

---

## 六、与后端架构的映射

```
model_provider
    ├── protocol_type → LlmProviderFactory 路由到对应 Provider 实现
    ├── api_base_url  → OpenAiCompatibleProvider 请求地址
    ├── auth_type     → AuthStrategyFactory 选择认证策略
    ├── api_key       → AuthStrategy 注入请求头
    └── auth_config   → 额外 Header/Query 参数透传

model_provider_status
    └── health_status → 管理后台列表页展示、Agent 创建时模型过滤

model_config
    ├── model_id      → LLM API 调用参数
    ├── max_tokens    → 请求构建器参数 + 前端表单默认值
    ├── context_window → Token 管理/截断策略参考
    └── capabilities  → 前端模型选择器过滤 + 后端调用前能力校验
```

---

*文档完*
