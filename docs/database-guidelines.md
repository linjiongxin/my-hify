# 数据库规范（PostgreSQL）

> 详细规范见本文档，AI 生成 SQL 时必须遵循

---

## 通用字段（所有表必须）

```sql
id BIGSERIAL PRIMARY KEY,
created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
created_by BIGINT,
updated_by BIGINT
```

### 自动更新触发器

```sql
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- 每个表创建时添加
CREATE TRIGGER update_表名_updated_at
    BEFORE UPDATE ON 表名
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
```

---

## 字段类型

| 场景 | 类型 | 禁用 |
|------|------|------|
| 主键/外键 | `BIGSERIAL` / `BIGINT` | INT |
| 短文本 | `VARCHAR(n)` | - |
| 长文本 | `TEXT` | VARCHAR(65535) |
| JSON | `JSONB`（支持索引）| JSON |
| 向量 | `VECTOR(1536)` | - |
| 布尔 | `BOOLEAN` | TINYINT(1) |
| 时间戳 | `TIMESTAMP` / `TIMESTAMPTZ` | DATETIME |
| 枚举 | `VARCHAR` + CHECK | - |
| 金额/精确数 | `DECIMAL(19,4)` | FLOAT/DOUBLE |

---

## 索引原则

**必须创建索引：**
- 主键（自动创建）
- 外键字段（手动创建）
- WHERE 条件高频字段
- ORDER BY 字段
- JSONB 查询字段（GIN 索引）

**命名规范：**
- 普通索引：`idx_{表名}_{字段名}`
- 唯一索引：`uk_{表名}_{字段名}`
- GIN 索引：`gin_{表名}_{字段名}`
- 向量索引：`vec_{表名}_{字段名}_{类型}`

**向量索引选择：**
```sql
-- 数据量 < 100万：ivfflat（内存友好）
CREATE INDEX ON rag_chunks
    USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);

-- 数据量 > 100万：hnsw（速度优先，内存大）
CREATE INDEX ON rag_chunks
    USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);
```

---

## 大表策略

| 表名 | 预估行数 | 策略 |
|------|---------|------|
| `chat_message` | 50万/年 | **按月分区** |
| `rag_chunk` | 100万/年 | **分区** + ivfflat 向量索引 |
| `workflow_execution` | 20万/年 | 按需分区 |
| 其他 | < 10万 | 普通表 |

**分区表示例：**
```sql
CREATE TABLE chat_message (
    id BIGSERIAL,
    session_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    content TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id, created_at)
) PARTITION BY RANGE (created_at);

-- 创建分区
CREATE TABLE chat_message_2024_01 PARTITION OF chat_message
    FOR VALUES FROM ('2024-01-01') TO ('2024-02-01');
```

---

## 分页规范

**浅分页（< 100页）：**
```sql
SELECT * FROM chat_message
WHERE session_id = 'xxx'
ORDER BY created_at DESC
LIMIT 20 OFFSET 40;
```

**深分页/消息历史（游标模式）：**
```sql
SELECT * FROM chat_message
WHERE session_id = 'xxx'
  AND (created_at, id) < ('2024-01-15 10:00:00', 12345)
ORDER BY created_at DESC, id DESC
LIMIT 20;
```

**禁止：** `OFFSET > 10000` 深分页

---

## 慢查询排查

```sql
-- 查看执行计划
EXPLAIN (ANALYZE, BUFFERS) SELECT ...;

-- 查看表大小
SELECT pg_size_pretty(pg_total_relation_size('chat_message'));

-- 查看缺失索引（顺序扫描过多）
SELECT tablename, seq_scan, idx_scan FROM pg_stat_user_tables;

-- 查看索引使用情况
SELECT indexname, idx_scan, idx_tup_read
FROM pg_stat_user_indexes
WHERE schemaname = 'public'
ORDER BY idx_scan DESC;
```

---

## 表名与实体类名映射规范

**核心原则：数据库表名必须与对应的 Java Entity 类名保持一致（下划线命名 ↔ 大驼峰命名）。**

| 表名 | Entity 类名 | 说明 |
|------|------------|------|
| `model_provider` | `ModelProvider` | 正确 |
| `model_config` | `ModelConfig` | 正确 |
| `model_config` | `Model` | **错误** — 表名与类名语义不一致 |
| `sys_user` | `SysUser` | 正确 |

**强制要求：**
- 新建表时，Entity 类名必须能直接反推出表名（去掉下划线并转为大驼峰即可）
- 禁止用泛化名称（如 `Model`、`Config`、`Data`）指代具体的业务表
- Entity 类必须加 `@TableName("exact_table_name")` 显式声明映射关系

---

## 建表模板

```sql
-- 1. 启用 pgvector
CREATE EXTENSION IF NOT EXISTS vector;

-- 2. 创建表
CREATE TABLE example_table (
    id BIGSERIAL PRIMARY KEY,
    -- 业务字段...
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_by BIGINT,
    updated_by BIGINT
);

-- 3. 添加触发器
CREATE TRIGGER update_example_table_updated_at
    BEFORE UPDATE ON example_table
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- 4. 创建索引
CREATE INDEX idx_example_table_created_by ON example_table(created_by);
```

---

## 配置来源规范

### 单一来源原则

**运行时配置必须以数据库为唯一可信来源，禁止在代码中硬编码业务可变配置。**

| 场景 | 正确做法 | 错误做法 |
|------|---------|---------|
| 模型提供商列表 | 从 `model_provider` 表读取 | 写死在枚举或 `application.yml` |
| 模型参数（温度、TopP） | 从 `model_config` 表读取 | 在代码里写死默认值 |
| 认证方式切换 | 由 `auth_type` 字段驱动 | 用 `if-else` 硬编码判断 |
| 开关/阈值 | 从配置表读取 | 用常量类写死 |

**例外情况（允许硬编码）：**
- 纯技术常量：HTTP 连接池大小、线程池核心数、超时秒数
- 与业务无关的框架默认值

**强制要求：**
- 所有业务配置必须能在不重新部署的情况下，通过修改数据库生效
- 需要缓存的配置，必须实现缓存刷新机制（如 @CacheEvict 或定时同步）

---

*规范完*
