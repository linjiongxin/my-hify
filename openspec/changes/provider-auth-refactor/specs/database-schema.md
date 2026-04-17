# 数据库 Schema 规范

## 表结构

### model_provider

厂商定义与鉴权配置。

```sql
CREATE TABLE IF NOT EXISTS model_provider (
    id BIGINT PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    code VARCHAR(32) NOT NULL,
    protocol_type VARCHAR(32) DEFAULT 'openai_compatible',
    api_base_url VARCHAR(256) NOT NULL,
    auth_type VARCHAR(32) DEFAULT 'BEARER',
    api_key VARCHAR(512),
    auth_config JSONB DEFAULT '{}',
    enabled BOOLEAN DEFAULT TRUE,
    sort_order INT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_by BIGINT,
    updated_by BIGINT
);
```

**索引：**
- `uk_model_provider_code` UNIQUE ON `code` WHERE `deleted = FALSE`

---

### model_config

模型定义与能力参数。

```sql
CREATE TABLE IF NOT EXISTS model_config (
    id BIGINT PRIMARY KEY,
    provider_id BIGINT NOT NULL,
    name VARCHAR(64) NOT NULL,
    model_id VARCHAR(64) NOT NULL,
    max_tokens INT DEFAULT 4096,
    context_window INT DEFAULT 8192,
    capabilities JSONB DEFAULT '{"chat":true,"streaming":true,"vision":false,"toolCalling":false,"reasoning":false,"jsonMode":false}',
    input_price_per_1m DECIMAL(10,6),
    output_price_per_1m DECIMAL(10,6),
    default_model BOOLEAN DEFAULT FALSE,
    enabled BOOLEAN DEFAULT TRUE,
    sort_order INT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_by BIGINT,
    updated_by BIGINT
);
```

**索引：**
- `idx_model_config_provider_id` ON `provider_id`
- `uk_model_config_model_id` UNIQUE ON `model_id` WHERE `deleted = FALSE`
- `idx_model_config_capabilities` GIN ON `capabilities`

---

### model_provider_status

运行时健康状态，与 `model_provider` 1:1。

```sql
CREATE TABLE IF NOT EXISTS model_provider_status (
    provider_id BIGINT PRIMARY KEY REFERENCES model_provider(id) ON DELETE CASCADE,
    health_status VARCHAR(16) DEFAULT 'unknown',
    health_checked_at TIMESTAMP,
    health_latency_ms INTEGER,
    health_error_msg TEXT,
    total_requests BIGINT DEFAULT 0,
    failed_requests BIGINT DEFAULT 0,
    last_error_at TIMESTAMP,
    last_error_code VARCHAR(64),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

**索引：**
- `idx_provider_status_health` ON `health_status`

## 初始化数据

已预置 5 家提供商（OpenAI、DeepSeek、Ollama、通义千问、Azure OpenAI）及 9 个模型。

详见 `docs/sql/init/hify-schema.sql`。
