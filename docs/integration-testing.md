# Hify 集成测试规范

本文档详细说明 Hify 项目的集成测试架构、编写规范和运行方式。

---

## 为什么不用 H2

项目使用 PostgreSQL 专有特性，H2 不兼容：

| 特性 | H2 支持 | 说明 |
|------|--------|------|
| pgvector 扩展 | ❌ | 向量检索核心依赖 |
| plpgsql 触发器函数 | ❌ | `update_updated_at_column()` |
| JSONB 类型 + GIN 索引 | ❌ | capabilities、config 字段 |
| 部分索引（`WHERE deleted = FALSE`） | ❌ | 唯一约束实现 |

**结论**：集成测试必须连接真实 PostgreSQL，使用独立的 `hify_test` 数据库。

---

## 三层测试架构

```
端到端集成测试 (E2E)    ← hify-server，@SpringBootTest(WebEnvironment.RANDOM_PORT)
    ↓
切片集成测试             ← 各模块，@SpringBootTest(WebEnvironment.NONE)，Mapper + Service
    ↓
单元测试                 ← 各模块，Mockito，Service 分支逻辑（已有 82 个）
```

| 层级 | 范围 | 启动 Spring | 连接真实 DB | 典型场景 |
|------|------|-----------|-----------|---------|
| **E2E** | 完整 API 链路 | ✅ | ✅ | MockMvc 调用 Controller → Service → Mapper → DB |
| **切片** | 模块内多组件 | ✅ | ✅ | Mapper XML 执行、Service 级联事务 |
| **单元** | 单个类/方法 | ❌ | ❌ | Mockito mock 所有依赖，毫秒级 |

---

## 数据库隔离策略

| 环境 | 数据库 | 用途 |
|------|--------|------|
| 开发 | `hify` | 日常开发，Docker Compose 默认创建 |
| 测试 | `hify_test` | 集成测试专用，与开发数据完全隔离 |

### 隔离机制

1. **物理隔离**：`hify` 和 `hify_test` 是独立的 PostgreSQL 数据库
2. **事务回滚**：每个测试方法加 `@Transactional`，测试后自动回滚
3. **数据重置**：`@Sql` 在每个测试方法前执行 `TRUNCATE` + `INSERT` 固定种子数据

---

## 环境准备

### 首次运行（或清理后重建）

```bash
# 1. 启动 PostgreSQL + Redis
docker compose up -d

# 2. 创建测试数据库并初始化 schema（只需执行一次）
chmod +x scripts/init-test-db.sh
./scripts/init-test-db.sh
```

### 日常运行

```bash
# 直接运行全部测试（集成测试 + 单元测试）
mvn test

# 仅运行集成测试（*IT.java）
mvn test -Dtest="*IT"

# 仅运行单元测试（*Test.java）
mvn test -Dtest="*Test"

# 运行单个集成测试类
mvn test -Dtest="ModelProviderServiceImplIT"
```

---

## 测试基类

### 端到端测试基类（hify-server）

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:postgresql://localhost:5432/hify_test",
    "spring.data.redis.host=localhost"
})
public abstract class IntegrationTestBase { }
```

配置说明：
- `RANDOM_PORT`：避免端口冲突
- `@AutoConfigureMockMvc`：注入 MockMvc 进行 HTTP 模拟
- `@Transactional`：每个测试后回滚数据库
- `@TestPropertySource`：覆盖数据源为 `hify_test`

### 切片测试基类（hify-module-model）

```java
@SpringBootTest(classes = ModelTestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Transactional
@Sql(scripts = "/sql/model-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public abstract class ModelModuleTestBase { }
```

配置说明：
- `WebEnvironment.NONE`：不启动 Web 容器，加速
- `@Sql`：每个测试方法前注入固定测试数据
- `ModelTestApplication`：模块级 Spring Boot 应用入口（扫描 `com.hify.model`）

---

## 测试数据管理

### 种子数据 SQL

文件：`hify-module-model/src/test/resources/sql/model-test-data.sql`

```sql
TRUNCATE TABLE model_provider CASCADE;
TRUNCATE TABLE model_config CASCADE;
TRUNCATE TABLE model_provider_status CASCADE;

INSERT INTO model_provider (id, name, code, protocol_type, api_base_url, auth_type, api_key, auth_config, enabled, sort_order, created_at, updated_at, deleted)
VALUES (1, 'OpenAI', 'openai', 'openai_compatible', 'https://api.openai.com/v1', 'BEARER', 'sk-test', '{}', TRUE, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
       (2, 'DeepSeek', 'deepseek', 'openai_compatible', 'https://api.deepseek.com/v1', 'BEARER', 'sk-test', '{}', FALSE, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE);

INSERT INTO model_config (id, provider_id, name, model_id, max_tokens, context_window, capabilities, input_price_per_1m, output_price_per_1m, default_model, enabled, sort_order, created_at, updated_at, deleted)
VALUES (10, 1, 'GPT-4o', 'gpt-4o', 4096, 8192, '{"chat":true,"streaming":true}', 5.00, 15.00, TRUE, TRUE, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE),
       (11, 1, 'GPT-4o Mini', 'gpt-4o-mini', 4096, 8192, '{"chat":true,"streaming":true}', 0.15, 0.60, FALSE, TRUE, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, FALSE);

INSERT INTO model_provider_status (provider_id, health_status, health_checked_at, health_latency_ms, total_requests, failed_requests, updated_at)
VALUES (1, 'healthy', CURRENT_TIMESTAMP, 120, 1000, 10, CURRENT_TIMESTAMP);
```

### 数据设计原则

- **固定 ID**：使用 `id = 1, 2, 10, 11` 等固定值，断言时可硬编码
- **覆盖边界**：包含 `enabled = true/false`、`deleted = true/false` 的组合
- **关联完整性**：`model_provider_status.provider_id` 外键引用存在的 provider
- **每个测试方法独立**：`@Sql` 在每个方法前执行，不受其他测试影响

---

## 命名规范

```
被测类名 + IT   // 集成测试后缀为 IT，区别于单元测试的 Test

方法名：should<行为>_when<条件>_given<前置状态>
示例：
  shouldReturnVoWithHealthStatus_whenSelectProviderPage_givenJoinedData
  shouldCreateProvider_andInitializeStatus_whenGivenValidRequest
  shouldThrowBizException_whenGetProviderDetail_givenNonExistingId
```

---

## 各层测试策略

| 层级 | 测试重点 | 工具/方式 |
|------|---------|----------|
| **api/** 接口 | 契约断言，不重复测试实现 | 无需测试（由 Service 测试覆盖） |
| **service/** | 核心业务逻辑（单元测试）+ 级联事务（集成测试） | Mockito（单元）/ `@SpringBootTest`（集成） |
| **mapper/** | 复杂 SQL（LEFT JOIN、子查询、分页）的正确性 | `@SpringBootTest` + 真实 PostgreSQL |
| **controller/** | 入参校验、HTTP 状态码、异常映射 | MockMvc（E2E 测试覆盖） |

### Mapper 集成测试示例

```java
@SpringBootTest(classes = ModelTestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Transactional
@Sql(scripts = "/sql/model-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class ModelProviderMapperIT {

    @Autowired
    private ModelProviderMapper modelProviderMapper;

    @Test
    void shouldReturnVoWithHealthStatus_whenSelectProviderPage_givenJoinedData() {
        Page<ModelProviderVO> page = new Page<>(1, 10);
        Page<ModelProviderVO> result = modelProviderMapper.selectProviderPage(page, false);

        // 验证 LEFT JOIN 结果：有 status 记录的 provider health_status = 'healthy'
        ModelProviderVO openai = result.getRecords().stream()
                .filter(p -> "openai".equals(p.getCode()))
                .findFirst()
                .orElseThrow();
        assertThat(openai.getHealthStatus()).isEqualTo("healthy");

        // 无 status 记录的 provider health_status = null
        ModelProviderVO deepseek = result.getRecords().stream()
                .filter(p -> "deepseek".equals(p.getCode()))
                .findFirst()
                .orElseThrow();
        assertThat(deepseek.getHealthStatus()).isNull();
    }
}
```

### Service 集成测试示例

```java
@Test
void shouldCreateProvider_andInitializeStatus_whenGivenValidRequest() {
    ModelProviderCreateRequest request = new ModelProviderCreateRequest();
    request.setName("Qwen");
    request.setCode("qwen");
    // ...

    Long id = modelProviderService.createProvider(request);

    // 验证级联插入：status 记录已自动创建
    ModelProviderStatus status = modelProviderStatusMapper.selectById(id);
    assertThat(status).isNotNull();
    assertThat(status.getHealthStatus()).isEqualTo("unknown");
}
```

### E2E API 测试示例

```java
@SpringBootTest(classes = HifyApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(scripts = "classpath:sql/model-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class ModelProviderApiIT extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnProviderDetail_whenGetById_givenExistingId() throws Exception {
        // 注意：MockMvc 自动处理 server.servlet.context-path=/api
        // 请求路径不需要加 /api 前缀
        mockMvc.perform(get("/model-provider/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("OpenAI"));
    }
}
```

---

## MockMvc 注意事项

Spring Boot 的 `MockMvc` 会自动处理 `server.servlet.context-path=/api` 配置。

| 实际 URL | MockMvc 请求路径 | 说明 |
|---------|-----------------|------|
| `GET /api/model-provider/1` | `get("/model-provider/1")` | ✅ 正确 |
| `GET /api/model-provider/1` | `get("/api/model-provider/1")` | ❌ 404 |

---

## 测试通过标准

- **新增功能**：Service 层核心逻辑必须有单元测试，分支覆盖 ≥ 80%
- **Mapper 复杂 SQL**：新增或修改 XML 必须配套集成测试
- **跨模块级联操作**：涉及多表事务的必须配套集成测试
- **Bug 修复**：必须先写复现测试，再修复代码
- **PR 提交前**：`mvn test` 必须全部通过，不允许跳过
- **禁止**：不写测试直接提交实现代码；测试通过后再改实现却不更新测试

---

## 文件清单

| 路径 | 说明 |
|------|------|
| `scripts/init-test-db.sh` | 一键初始化测试数据库 |
| `hify-server/src/test/resources/application-test.yml` | 测试环境配置（数据源、Redis、日志） |
| `hify-server/src/test/java/com/hify/server/IntegrationTestBase.java` | 端到端测试基类 |
| `hify-module-model/src/test/java/com/hify/model/ModelTestApplication.java` | model 模块测试入口 |
| `hify-module-model/src/test/resources/sql/model-test-data.sql` | 模型模块测试种子数据 |
| `hify-server/src/test/java/com/hify/server/api/ModelProviderApiIT.java` | E2E API 测试示例 |
| `hify-module-model/src/test/java/com/hify/model/mapper/ModelProviderMapperIT.java` | Mapper 集成测试示例 |
| `hify-module-model/src/test/java/com/hify/model/service/impl/ModelProviderServiceImplIT.java` | Service 集成测试示例 |
| `hify-module-model/src/test/java/com/hify/model/service/impl/ModelConfigServiceImplIT.java` | Service 集成测试示例 |
