# Provider 模块数据模型重构

## 背景与问题

原有 demo 实现中：

- `OpenAiCompatibleProvider` 仅支持标准 OpenAI 协议，认证方式写死为 `Authorization: Bearer {apiKey}`
- 厂商和配置混在一起，新增一家兼容厂商需要改 Java 代码
- 健康状态没有明确的存储位置
- 模型能力与模型元信息混在同一层，无法做能力过滤

## 目标

1. **配置驱动**：新增一个 OpenAI 兼容厂商，只需插数据库记录，不写 Java 代码
2. **鉴权统一**：Bearer、API Key、无认证、自定义 Header 等多种鉴权方式能用同一套结构存储
3. **状态隔离**：健康状态变化频繁，不应和稳定的厂商元信息绑在一起
4. **模型管理**：一个供应商下多个模型，每个模型有独立的能力参数

## 设计方案

### 数据模型：三表方案

- **`model_provider`**：稳定元信息 + 鉴权配置（运行时以只读为主）
- **`model_provider_status`**：易变健康状态（高频更新，完全隔离）
- **`model_config`**：模型能力与参数（配置层）

选择三表而非四表的理由：
- 内部平台一个厂商通常只配一个 API Key，无需单独的 `config` 拆表
- `status` 单独成表职责边界清晰，也方便未来扩展 QPS、错误统计等运行时指标
- 总共 3 张表，JOIN 最多两层，对 PostgreSQL 完全无压力

### 鉴权配置（JSONB）

不同 `auth_type` 对应不同的 `auth_config` 结构：

| auth_type | 示例 | 适用厂商 |
|-----------|------|----------|
| **BEARER** | `{"apiKey": "sk-xxx"}` | OpenAI, DeepSeek, Qwen, Kimi |
| **API_KEY** | `{"apiKey": "xxx", "headerName": "api-key", "prefix": ""}` | Azure OpenAI |
| **NONE** | `{}` | Ollama, 部分内网 vLLM |
| **CUSTOM** | `{"headers": {"X-Auth": "xxx"}}` | 私有网关、定制部署 |

### 模型能力矩阵（JSONB）

标准 `capabilities` Schema：

```json
{
    "chat": true,
    "streaming": true,
    "vision": false,
    "toolCalling": true,
    "reasoning": false,
    "jsonMode": true
}
```

### 架构映射

```
model_provider
    ├── protocol_type → LlmProviderFactory 路由到对应 Provider 实现
    ├── api_base_url  → OpenAiCompatibleProvider 请求地址
    ├── auth_type     → applyAuthHeaders() 选择认证策略
    ├── api_key       → 主密钥
    └── auth_config   → 额外 Header/Query 参数透传

model_provider_status
    └── health_status → 管理后台列表页展示、Agent 创建时模型过滤

model_config
    ├── model_id      → LLM API 调用参数
    ├── max_tokens    → 请求构建器参数 + 前端表单默认值
    ├── context_window → Token 管理/截断策略参考
    └── capabilities  → 前端模型选择器过滤 + 后端调用前能力校验
```

### 模块间调用规范（重构后）

- **`api/` 层**：新增 `ModelProviderApi`、`ModelConfigApi`，返回 `ModelProviderDTO`、`ModelConfigDTO`
- **跨模块调用**：`hify-server` 的 `LlmProviderHealthIndicator` 通过 `ModelProviderApi` 获取数据，不再直接注入 Service/引用 Entity
- **常量管理**：`ModelConstants` 集中管理认证类型、协议类型、Header 名称、健康状态等字符串常量
- **异常规范**：LLM 网关与 Provider 实现统一抛出带 `ResultCode` 的 `BizException`，禁止泛化 `RuntimeException`
- **布尔命名**：`model_config` 表中 `is_default` 统一改为 `default_model`，避免 Lombok/序列化歧义

## 关键决策

| 问题 | 方案 | 理由 |
|------|------|------|
| 多种鉴权差异怎么统一存储 | `auth_type` 标签 + `auth_config` JSONB | 结构灵活，新增鉴权方式不改表结构 |
| 一个供应商下多个模型怎么管理 | `model_config.provider_id` 外键 + `capabilities` JSONB | 清晰一对多，能力矩阵支持筛选与校验 |
| 供应商健康状态怎么表示 | 独立 `model_provider_status` 表 | 高频更新与稳定元信息隔离，避免锁竞争 |
| 为什么不保留 model_provider_config 表 | 内部平台一个厂商通常只有一个配置 | 多拆一张表就多一个 JOIN 和维护成本 |
| 新增兼容厂商需要做什么 | 插 2-3 条 SQL | 不需要写任何 Java 代码，真正做到配置驱动 |

## 影响范围

- **数据库**：PostgreSQL schema 变更（`model_provider`、`model_config`、`model_provider_status`）；`model_config.is_default` 重命名为 `default_model`
- **后端**：`hify-module-model` 新增实体、Mapper、Service、Controller、api/ 层 DTO、`ModelConstants`；`hify-common-web` 新增 `JsonbTypeHandler`；`BizException` 扩展 `(ResultCode, String, Throwable)` 构造器
- **前端**：`hify-web` 模型提供商管理页对接真实 API，替换 mock 数据
- **网关**：`LlmGatewayServiceImpl` 从数据库读取 provider 配置并路由
- **架构修复**：`hify-server` 的 `LlmProviderHealthIndicator` 改走 `ModelProviderApi`，消除跨模块直接引用 Service/Entity

## 产出物

- 子规范：
  - [数据库 Schema 规范](specs/database-schema.md)
  - [后端 API 规范](specs/backend-api.md)
  - [前端页面规范](specs/frontend-provider.md)
  - [LLM 网关规范](specs/llm-gateway.md)
- OpenSpec 变更目录：`openspec/changes/provider-auth-refactor/`
- 代码提交：`81788d0` + 后续重构
