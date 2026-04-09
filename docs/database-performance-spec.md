# Hify 数据库性能规范

> PostgreSQL 15+ + pgvector 扩展
>
> 目标：50 人内部团队，支持 AI 场景（向量检索、流式对话）

---

## 一、通用字段约定（所有表必须）

```sql
-- 通用字段模板，所有业务表必须包含
CREATE TABLE example_table (
    -- 主键：统一使用 BIGSERIAL
    id BIGSERIAL PRIMARY KEY,

    -- 业务字段...

    -- 通用审计字段（必须）
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,                          -- 创建人 ID
    updated_by BIGINT,                          -- 更新人 ID
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,  -- 逻辑删除标记

    -- 租户/空间隔离（预留，当前单租户固定 0）
    tenant_id BIGINT NOT NULL DEFAULT 0
);

-- 更新 updated_at 的触发器（所有表通用）
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_example_table_updated_at
    BEFORE UPDATE ON example_table
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
```

### 字段类型规范

| 场景 | 推荐类型 | 说明 |
|------|---------|------|
| 主键 | `BIGSERIAL` | 64 位自增，满足 50 人团队增长 |
| 外键 | `BIGINT` | 与主键类型一致 |
| 短文本 | `VARCHAR(n)` | 用户名、标题等，明确长度限制 |
| 长文本 | `TEXT` | 对话内容、Prompt、文档原文 |
| JSON | `JSONB` | 必须 JSONB（支持索引），非 JSON |
| 布尔 | `BOOLEAN` | 禁用 TINYINT(1) 模拟 |
| 时间戳 | `TIMESTAMP` | 带时区用 `TIMESTAMPTZ`，统一 UTC |
| 枚举 | `VARCHAR` + CHECK | 简单枚举，复杂用关联表 |
| 金额/精确数 | `DECIMAL(19,4)` | 避免 FLOAT/DOUBLE |
| 向量 | `VECTOR(1536)` | pgvector 扩展，1536 维（OpenAI）|

---

## 二、索引设计原则

### 2.1 索引创建 checklist

**必须创建索引的场景：**
- [ ] 主键（自动创建）
- [ ] 外键字段（手动创建）
- [ ] WHERE 条件高频字段
- [ ] ORDER BY 字段
- [ ] 联合查询关联字段
- [ ] JSONB 查询字段（GIN 索引）
- [ ] 全文搜索字段（GIN + to_tsvector）

**禁止创建索引：**
- [ ] 低基数字段（如 status 只有 3 个值且分布不均）
- [ ] 频繁更新的字段（索引维护成本高）
- [ ] 小表（< 1000 行，全表扫描更快）

### 2.2 索引类型选择

```sql
-- B-Tree（默认）：等值、范围查询
CREATE INDEX idx_user_email ON users(email);
CREATE INDEX idx_time_range ON chat_message(created_at DESC);

-- 联合索引：最左前缀原则
CREATE INDEX idx_session_time ON chat_message(session_id, created_at DESC);

-- GIN：JSONB、数组、全文搜索
CREATE INDEX idx_metadata ON agent_config USING GIN (metadata);
CREATE INDEX idx_content_search ON documents
    USING GIN(to_tsvector('chinese', content));  -- 中文全文搜索

-- 向量索引（pgvector）：相似度检索
CREATE INDEX ON document_chunks
    USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);  -- lists = 行数 / 1000

-- 部分索引：减少索引大小
CREATE INDEX idx_active_users ON users(email) WHERE is_deleted = FALSE;

-- 唯一索引：业务唯一约束
CREATE UNIQUE INDEX idx_provider_model
    ON model(provider_key, model_name)
    WHERE is_deleted = FALSE;
```

### 2.3 向量索引规范（pgvector）

```sql
-- 数据量 < 100 万：ivfflat（内存友好）
CREATE INDEX ON rag_chunks
    USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);

-- 数据量 > 100 万：hnsw（速度优先，内存大）
CREATE INDEX ON rag_chunks
    USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);

-- 查询时指定 ef（hnsw 用）
SET hnsw.ef_search = 100;  -- 默认 40，越大精度越高越慢
```

### 2.4 索引命名规范

```sql
-- 格式：idx_{表名}_{字段名}
CREATE INDEX idx_chat_message_session_id ON chat_message(session_id);
CREATE INDEX idx_chat_message_created_at ON chat_message(created_at DESC);

-- 联合索引：idx_{表名}_{字段1}_{字段2}
CREATE INDEX idx_workflow_node_wf_id_sort ON workflow_node(workflow_id, sort_order);

-- 唯一索引：uk_{表名}_{字段名}
CREATE UNIQUE INDEX uk_model_provider_name ON model_provider(provider_key);

-- GIN 索引：gin_{表名}_{字段名}
CREATE INDEX gin_agent_metadata ON agent USING GIN (metadata);

-- 向量索引：vec_{表名}_{字段名}_{类型}
CREATE INDEX vec_rag_chunks_embedding_ivf ON rag_chunks USING ivfflat (embedding vector_cosine_ops);
```

---

## 三、大表预判和应对策略

### 3.1 大表定义（Hify 场景）

| 表名 | 预估行数（50 人/年） | 大表阈值 | 是否分区 |
|------|---------------------|---------|---------|
| `chat_message` | 50 万 | 100 万 | 是 |
| `chat_session` | 5 万 | 50 万 | 否 |
| `rag_document` | 1 万 | 10 万 | 否 |
| `rag_chunk` | 100 万 | 500 万 | 是 |
| `workflow_execution` | 20 万 | 100 万 | 是 |
| `mcp_tool_call` | 50 万 | 100 万 | 是 |

### 3.2 分区策略

```sql
-- chat_message 按时间范围分区（月分区）
CREATE TABLE chat_message (
    id BIGSERIAL,
    session_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    content TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- 其他字段...
    PRIMARY KEY (id, created_at)  -- 分区键必须包含在主键中
) PARTITION BY RANGE (created_at);

-- 创建分区（提前创建 3 个月）
CREATE TABLE chat_message_2024_01 PARTITION OF chat_message
    FOR VALUES FROM ('2024-01-01') TO ('2024-02-01');
CREATE TABLE chat_message_2024_02 PARTITION OF chat_message
    FOR VALUES FROM ('2024-02-01') TO ('2024-03-01');

-- 默认分区（防止插入失败）
CREATE TABLE chat_message_default PARTITION OF chat_message DEFAULT;

-- 定时创建新分区（CronJob 或 pg_cron 扩展）
-- 清理旧分区（保留 6 个月）
DROP TABLE chat_message_2023_06;  -- 已过期数据
```

### 3.3 冷热数据分离

```sql
-- 热数据：最近 3 个月，频繁查询
-- 温数据：3-12 个月，偶尔查询
-- 冷数据：> 12 个月，归档到对象存储

-- 归档表（低配置，可压缩）
CREATE TABLE chat_message_archive (
    LIKE chat_message INCLUDING ALL
) WITH (fillfactor=70);  -- 预留空间用于 HOT 更新

-- 迁移脚本（定时执行）
INSERT INTO chat_message_archive
SELECT * FROM chat_message
WHERE created_at < NOW() - INTERVAL '3 months';

DELETE FROM chat_message
WHERE created_at < NOW() - INTERVAL '3 months';
```

### 3.4 大表维护操作

```sql
-- 定期 VACUUM（PostgreSQL 特有，防止表膨胀）
VACUUM ANALYZE chat_message;  -- 手动执行
-- 或配置 autovacuum（已默认开启）

-- 更新统计信息（执行计划准确）
ANALYZE chat_message;

-- 重建索引（索引膨胀时）
REINDEX INDEX CONCURRENTLY idx_chat_message_session_id;

-- 查看表大小
SELECT
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as size
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;
```

---

## 四、分页查询注意事项

### 4.1 深分页问题

```sql
-- 坏写法：OFFSET 越大越慢（需扫描 OFFSET + LIMIT 行）
SELECT * FROM chat_message
ORDER BY created_at DESC
LIMIT 20 OFFSET 10000;  -- 慢！需扫描 10020 行

-- 好写法：基于游标/最后 ID
SELECT * FROM chat_message
WHERE created_at < '2024-01-15 10:00:00'  -- 上一页最后一条的时间
ORDER BY created_at DESC
LIMIT 20;

-- 或基于主键游标（配合前端传递 last_id）
SELECT * FROM chat_message
WHERE id < 1000000  -- 上一页最后一条的 ID
ORDER BY id DESC
LIMIT 20;
```

### 4.2 推荐分页模式

```sql
-- 场景 1：常规列表（有限深度，< 100 页）
-- 直接用 OFFSET，简单
SELECT * FROM chat_message
WHERE session_id = 'xxx'
ORDER BY created_at DESC
LIMIT 20 OFFSET 40;

-- 场景 2：消息历史（可能很深）
-- 使用游标分页（Keyset Pagination）
SELECT * FROM chat_message
WHERE session_id = 'xxx'
  AND (created_at, id) < ('2024-01-15 10:00:00', 12345)  -- 复合游标
ORDER BY created_at DESC, id DESC
LIMIT 20;

-- 场景 3：导出/同步（流式，不分页）
-- 使用游标
DECLARE cur CURSOR FOR SELECT * FROM chat_message WHERE ...;
FETCH 1000 FROM cur;
```

### 4.3 分页查询必须加索引

```sql
-- 慢查询根因：缺少 (session_id, created_at) 联合索引
EXPLAIN ANALYZE
SELECT * FROM chat_message
WHERE session_id = 12345
ORDER BY created_at DESC
LIMIT 20;

-- 必须创建：
CREATE INDEX idx_chat_message_session_created
    ON chat_message(session_id, created_at DESC);
```

### 4.4 COUNT(*) 优化

```sql
-- 坏写法：精确 COUNT 慢
SELECT COUNT(*) FROM chat_message WHERE session_id = 'xxx';

-- 好写法 1：估算（返回近似值，快）
SELECT reltuples::BIGINT AS estimate
FROM pg_class
WHERE relname = 'chat_message';

-- 好写法 2：缓存计数（应用层维护）
-- 会话表冗余 message_count 字段，增删时更新

-- 好写法 3：限制深度（产品层妥协）
-- "显示前 1000 条，更多请筛选"
SELECT COUNT(*) FROM (
    SELECT 1 FROM chat_message
    WHERE session_id = 'xxx'
    LIMIT 1000
) t;
```

---

## 五、AI 建表可直接执行的模板

### 5.1 用户/权限模块

```sql
-- 用户表
CREATE TABLE sys_user (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(100),
    role VARCHAR(20) NOT NULL DEFAULT 'USER', -- ADMIN/USER
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    tenant_id BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uk_sys_user_username ON sys_user(username) WHERE is_deleted = FALSE;
CREATE INDEX idx_sys_user_role ON sys_user(role) WHERE is_deleted = FALSE;

-- 触发器：自动更新 updated_at
CREATE TRIGGER update_sys_user_updated_at
    BEFORE UPDATE ON sys_user
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
```

### 5.2 对话模块（大表，需分区）

```sql
-- 会话表
CREATE TABLE chat_session (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES sys_user(id),
    agent_id BIGINT NOT NULL,
    title VARCHAR(200),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_chat_session_user ON chat_session(user_id, created_at DESC);
CREATE INDEX idx_chat_session_agent ON chat_session(agent_id);

CREATE TRIGGER update_chat_session_updated_at
    BEFORE UPDATE ON chat_session
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- 消息表（大表，按时间分区）
CREATE TABLE chat_message (
    id BIGSERIAL,
    session_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL, -- user/assistant/system
    content TEXT NOT NULL,
    tokens_used INTEGER DEFAULT 0,
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id, created_at)
) PARTITION BY RANGE (created_at);

-- 初始分区
CREATE TABLE chat_message_default PARTITION OF chat_message DEFAULT;

-- 索引（在每个分区上自动创建）
CREATE INDEX idx_chat_message_session ON chat_message(session_id, created_at DESC);

-- 注意：分区表的触发器需在每个分区上创建，或通过父表继承
```

### 5.3 RAG 知识库模块（含向量）

```sql
-- 启用 pgvector
CREATE EXTENSION IF NOT EXISTS vector;

-- 知识库
CREATE TABLE rag_knowledge_base (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    embedding_model VARCHAR(50) DEFAULT 'text-embedding-3-small',
    chunk_size INTEGER DEFAULT 500,
    chunk_overlap INTEGER DEFAULT 50,
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_rag_kb_created_by ON rag_knowledge_base(created_by);

CREATE TRIGGER update_rag_kb_updated_at
    BEFORE UPDATE ON rag_knowledge_base
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- 文档
CREATE TABLE rag_document (
    id BIGSERIAL PRIMARY KEY,
    kb_id BIGINT NOT NULL REFERENCES rag_knowledge_base(id),
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(20), -- pdf/txt/md
    file_size BIGINT,
    content TEXT, -- 清洗后的纯文本
    status VARCHAR(20) DEFAULT 'PENDING', -- PENDING/PROCESSING/DONE/ERROR
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_rag_doc_kb ON rag_document(kb_id, status);

-- 文档分块（大表 + 向量）
CREATE TABLE rag_chunk (
    id BIGSERIAL PRIMARY KEY,
    doc_id BIGINT NOT NULL REFERENCES rag_document(id),
    kb_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    embedding VECTOR(1536), -- OpenAI embedding 维度
    chunk_index INTEGER, -- 第几个分块
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

-- B-Tree 索引
CREATE INDEX idx_rag_chunk_doc ON rag_chunk(doc_id);
CREATE INDEX idx_rag_chunk_kb ON rag_chunk(kb_id);

-- 向量索引（根据数据量选择）
-- 方案 A：数据量 < 100 万，用 ivfflat
CREATE INDEX ON rag_chunk
    USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);

-- 方案 B：数据量 > 100 万，用 hnsw（更快但内存大）
-- CREATE INDEX ON rag_chunk
--     USING hnsw (embedding vector_cosine_ops)
--     WITH (m = 16, ef_construction = 64);
```

### 5.4 工作流模块

```sql
-- 工作流定义
CREATE TABLE workflow (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    definition JSONB NOT NULL, -- 工作流 JSON 定义
    status VARCHAR(20) DEFAULT 'DRAFT', -- DRAFT/PUBLISHED/DISABLED
    version INTEGER DEFAULT 1,
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_workflow_status ON workflow(status, created_by);

CREATE TRIGGER update_workflow_updated_at
    BEFORE UPDATE ON workflow
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- 工作流执行（大表，可分区）
CREATE TABLE workflow_execution (
    id BIGSERIAL PRIMARY KEY,
    workflow_id BIGINT NOT NULL REFERENCES workflow(id),
    trigger_type VARCHAR(20), -- MANUAL/SCHEDULED/API
    status VARCHAR(20) NOT NULL DEFAULT 'RUNNING', -- RUNNING/SUCCESS/FAILED
    input_params JSONB,
    output_result JSONB,
    error_message TEXT,
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ended_at TIMESTAMP,
    created_by BIGINT,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_wf_exec_workflow ON workflow_execution(workflow_id, started_at DESC);
CREATE INDEX idx_wf_exec_status ON workflow_execution(status) WHERE status = 'RUNNING';
```

### 5.5 MCP 模块

```sql
-- MCP Server 配置
CREATE TABLE mcp_server (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    server_url VARCHAR(500) NOT NULL,
    transport_type VARCHAR(20) DEFAULT 'SSE', -- SSE/stdio
    status VARCHAR(20) DEFAULT 'ACTIVE', -- ACTIVE/ERROR/DISABLED
    tools_cache JSONB, -- 缓存的工具列表
    last_sync_at TIMESTAMP,
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE UNIQUE INDEX uk_mcp_server_url ON mcp_server(server_url) WHERE is_deleted = FALSE;

-- MCP 工具调用日志（大表，可分区）
CREATE TABLE mcp_tool_call (
    id BIGSERIAL PRIMARY KEY,
    server_id BIGINT NOT NULL REFERENCES mcp_server(id),
    tool_name VARCHAR(100) NOT NULL,
    request_params JSONB,
    response_result JSONB,
    status VARCHAR(20), -- SUCCESS/ERROR
    error_message TEXT,
    duration_ms INTEGER, -- 调用耗时
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_mcp_call_server ON mcp_tool_call(server_id, created_at DESC);
CREATE INDEX idx_mcp_call_time ON mcp_tool_call(created_at DESC);
```

---

## 六、慢查询排查手册

```sql
-- 查看当前慢查询（> 1秒）
SELECT
    pid,
    now() - query_start AS duration,
    query
FROM pg_stat_activity
WHERE state = 'active'
  AND now() - query_start > interval '1 second'
ORDER BY duration DESC;

-- 查看表统计信息（执行计划用）
ANALYZE chat_message;

-- 查看查询执行计划
EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON)
SELECT * FROM chat_message
WHERE session_id = 12345
ORDER BY created_at DESC
LIMIT 20;

-- 查看索引使用情况
SELECT
    schemaname,
    tablename,
    indexname,
    idx_scan,      -- 索引扫描次数
    idx_tup_read,  -- 通过索引读取的行数
    idx_tup_fetch  -- 通过索引获取的 live 行数
FROM pg_stat_user_indexes
WHERE schemaname = 'public'
ORDER BY idx_scan DESC;

-- 查看缺失索引的表（顺序扫描过多）
SELECT
    schemaname,
    tablename,
    seq_scan,      -- 顺序扫描次数
    seq_tup_read,  -- 顺序扫描读取的行数
    idx_scan       -- 索引扫描次数
FROM pg_stat_user_tables
WHERE schemaname = 'public'
  AND seq_scan > 1000
  AND (idx_scan IS NULL OR seq_scan > idx_scan * 10)
ORDER BY seq_tup_read DESC;
```

---

## 七、配置参数建议

```ini
# postgresql.conf（针对 50 人团队，2G 内存）

# 内存
shared_buffers = 512MB          # 共享缓冲区（25% 内存）
effective_cache_size = 1536MB   # 操作系统缓存估计（75% 内存）
work_mem = 4MB                  # 单个查询操作内存
maintenance_work_mem = 128MB    # 维护操作（VACUUM、索引）内存

# 并发
max_connections = 100           # 最大连接数（HikariCP 连接池 20 * 2 副本 + 余量）

# WAL（预写日志）
wal_buffers = 16MB
max_wal_size = 1GB
min_wal_size = 512MB

# 查询计划
effective_io_concurrency = 200  # SSD 存储
random_page_cost = 1.1          # SSD 存储

# 日志（开发/测试环境开启，生产按需）
log_min_duration_statement = 1000  # 记录 > 1s 的慢查询
log_line_prefix = '%t [%p]: [%l-1] user=%u,db=%d,app=%a,client=%h '
```

---

*文档完*
