# Hify 代码组织规范

> 多模块单体架构，具体可执行标准

---

## 一、模块清单与目录结构

```
hify/
├── hify-common/
│   ├── hify-common-core/          # 纯工具，无 Spring
│   └── hify-common-web/           # Web 公共组件
├── hify-module-model/             # 模型提供商管理
├── hify-module-agent/             # Agent 配置
├── hify-module-chat/              # 对话引擎
├── hify-module-rag/               # 知识库 RAG
├── hify-module-workflow/          # 工作流
├── hify-module-mcp/               # MCP 工具
└── hify-server/                   # 启动器
```

---

## 二、每个模块内部的分层结构（强制）

每个模块必须包含以下 6 个包，层级扁平，禁止嵌套超过 3 层：

```
hify-module-xxx/
└── src/main/java/com/hify/xxx/
    ├── api/                 # 【对外暴露】其他模块只能调用这一层
    ├── service/             # 【业务逻辑】本模块内部使用
    ├── mapper/              # 【数据访问】MyBatis 接口
    ├── entity/              # 【数据对象】MyBatis 实体、DTO、VO
    ├── controller/          # 【可选】本模块对外 HTTP 接口
    └── config/              # 【可选】本模块专属配置
```

---

## 三、每一层的职责边界（强制规范）

### 3.1 api/ 层 - 对外暴露的唯一入口

**存放位置**：`com.hify.xxx.api`

**必须包含**：
- `XxxApi.java` - 接口定义
- `XxxDTO.java` - 数据传输对象（所有字段必须注释用途）

**代码规范**：
```java
package com.hify.model.api;

/**
 * 模型提供商服务接口
 * 其他模块只能通过此接口访问 model 模块
 */
public interface ModelProviderApi {

    /**
     * 根据 ID 获取模型信息
     * @param modelId 模型 ID，不能为 null
     * @return 模型信息，不存在返回 null
     */
    ModelDTO getModelById(Long modelId);

    /**
     * 列出指定提供商的所有可用模型
     * @param providerKey 提供商唯一标识，如 "openai"
     * @return 模型列表，不会返回 null
     */
    List<ModelDTO> listModelsByProvider(String providerKey);

    /**
     * 调用模型生成对话（流式）
     * @param request 请求参数
     * @return SSE 事件流
     */
    SseEmitter streamChat(ChatRequestDTO request);
}

/**
 * 模型信息 DTO
 * 禁止包含实体类引用（如 ModelEntity），必须扁平化
 */
@Data
public class ModelDTO {
    /** 模型 ID */
    private Long id;
    /** 所属提供商 */
    private String providerKey;
    /** 模型原始名称，如 "gpt-4-turbo" */
    private String modelName;
    /** 显示名称 */
    private String displayName;
    /** 是否支持工具调用 */
    private Boolean functionCall;
    /** 上下文窗口大小 */
    private Integer contextWindow;
}
```

**禁止事项**（违反则代码审查不通过）：
- ❌ 返回 `ModelEntity` 等内部实体类
- ❌ 返回 `CompletableFuture`（使用同步返回或 SseEmitter）
- ❌ 暴露 `Page<T>` 等 MyBatis-Plus 内部类（转换为自定义 PageDTO）
- ❌ 方法参数超过 4 个（必须使用 DTO 封装）

---

### 3.2 service/ 层 - 业务逻辑实现

**存放位置**：`com.hify.xxx.service`

**文件命名**：`XxxService.java`（接口）+ `XxxServiceImpl.java`（实现）

**代码规范**：
```java
package com.hify.model.service;

/**
 * 模型提供商服务实现
 * 只能被本模块的 controller 或 api/impl 调用
 */
@Service
public class ModelProviderServiceImpl
    extends ServiceImpl<ModelProviderMapper, ModelProviderEntity>
    implements ModelProviderApi {  // 实现 api 接口

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private RestTemplate restTemplate;

    // 缓存操作必须封装在 service 层
    @Cacheable(value = "model", key = "#modelId")
    @Override
    public ModelDTO getModelById(Long modelId) {
        // 参数校验
        Assert.notNull(modelId, "modelId cannot be null");

        // 业务逻辑
        ModelEntity entity = modelMapper.selectById(modelId);
        if (entity == null) {
            return null;
        }

        // 转换为 DTO（禁止直接返回 entity）
        return convertToDTO(entity);
    }

    private ModelDTO convertToDTO(ModelEntity entity) {
        ModelDTO dto = new ModelDTO();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }
}
```

**禁止事项**：
- ❌ Service 之间直接调用（必须通过 api/ 层）
- ❌ 在 Service 中写 SQL（必须放在 Mapper XML）
- ❌ 在 Service 中操作 HttpServletRequest/Response

---

### 3.3 mapper/ 层 - 数据访问

**存放位置**：`com.hify.xxx.mapper`

**文件命名**：`XxxMapper.java` + `XxxMapper.xml`

**代码规范**：
```java
package com.hify.model.mapper;

/**
 * 模型提供商 Mapper
 * 禁止在其他模块中引用（即使在同一模块也优先走 service）
 */
@Mapper
public interface ModelProviderMapper extends BaseMapper<ModelProviderEntity> {

    /**
     * 根据提供商 key 查询所有模型
     * 复杂查询必须写 XML，禁止用 @Select 注解
     */
    List<ModelEntity> selectByProviderKey(@Param("providerKey") String providerKey);
}
```

**XML 位置**：`src/main/resources/mapper/ModelProviderMapper.xml`

**禁止事项**：
- ❌ 在 Service/Controller 中直接使用 Mapper（必须通过 Service）
- ❌ 在 Mapper 中写业务逻辑（只能是 CRUD）
- ❌ 返回 Map（必须返回 Entity 或自定义对象）

---

### 3.4 entity/ 层 - 数据对象

**存放位置**：`com.hify.xxx.entity`

**三类文件**：
- `XxxEntity.java` - MyBatis 实体（表结构映射）
- `XxxDTO.java` - 层间传输（api/ 层返回）
- `XxxVO.java` - 视图对象（controller 层返回给前端）

**代码规范**：
```java
package com.hify.model.entity;

/**
 * 模型提供商实体
 * 禁止包含任何业务逻辑
 */
@Data
@TableName("model_provider")
public class ModelProviderEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 提供商唯一标识 */
    private String providerKey;

    /** 显示名称 */
    private String name;

    /** API 基础 URL */
    private String apiBaseUrl;

    /** API 密钥（加密存储） */
    private String apiKey;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
```

**禁止事项**：
- ❌ 在 Entity 中写业务方法（如 `encryptApiKey()`）
- ❌ Entity 直接返回给前端（必须转 VO）
- ❌ 在其他模块引用本模块的 Entity

---

### 3.5 controller/ 层 - HTTP 接口（可选）

**存放位置**：`com.hify.xxx.controller`

**适用场景**：本模块有独立管理后台页面需要调用的接口

**职责边界（四件事）**：

| 序号 | 职责 | 说明 |
|------|------|------|
| 1 | **接收参数** | 解析 PathVariable、RequestParam、RequestBody |
| 2 | **入参校验** | 使用 `@Valid` 或 `Assert` 校验参数合法性 |
| 3 | **权限检查** | 使用 `@PreAuthorize` 或手动校验用户权限 |
| 4 | **调用并返回** | 调用 Service，直接 `Result.success(result)` 返回 |

**数据转换规则**：

- **默认**：Service 返回 DTO，Controller 直接包装返回，**不做字段转换**
- **例外**：需要特殊脱敏或组装时，才转 VO
- **底线**：禁止直接返回 Entity

**代码规范**：
```java
package com.hify.model.controller;

/**
 * 模型提供商管理接口
 * 路径前缀：/api/model
 */
@RestController
@RequestMapping("/api/model")
@RequiredArgsConstructor
public class ModelProviderController {

    // 只能注入本模块的 service
    private final ModelProviderService modelProviderService;

    /**
     * 创建模型提供商
     */
    @PostMapping("/providers")
    @PreAuthorize("hasRole('ADMIN')")           // 3. 权限检查
    public Result<ProviderDTO> createProvider(
            @Valid @RequestBody ProviderCreateRequest request  // 1. 接收参数 + 2. 入参校验
    ) {
        // 4. 调用 Service 并直接返回（Service 已脱敏敏感字段）
        return Result.success(modelProviderService.create(request));
    }

    /**
     * 列出所有提供商
     */
    @GetMapping("/providers")
    public Result<List<ProviderDTO>> listProviders() {
        // 直接返回 Service 结果
        return Result.success(modelProviderService.listAll());
    }

    /**
     * 手动参数校验示例（复杂场景）
     */
    @GetMapping("/providers/{id}")
    public Result<ProviderDTO> getProvider(@PathVariable Long id) {
        // 2. 手动入参校验
        Assert.notNull(id, "提供商ID不能为空");
        Assert.isTrue(id > 0, "提供商ID必须大于0");

        // 4. 直接返回 Service 结果
        return Result.success(modelProviderService.getById(id));
    }
}
```

**校验规则示例**：
```java
@Data
public class ProviderCreateRequest {

    @NotBlank(message = "提供商标识不能为空")
    @Pattern(regexp = "^[a-z0-9_]+$", message = "只能包含小写字母、数字、下划线")
    private String providerKey;

    @NotBlank(message = "显示名称不能为空")
    @Size(max = 50, message = "名称长度不能超过50字符")
    private String name;

    @NotBlank(message = "API 地址不能为空")
    @URL(message = "必须是有效的 URL")
    private String apiBaseUrl;

    @NotBlank(message = "API 密钥不能为空")
    private String apiKey;
}
```

**禁止事项**：
- ❌ 在 Controller 中写业务逻辑（如计算、状态判断、调用外部 HTTP）
- ❌ Controller 直接调用 Mapper
- ❌ 跨模块注入 Service（必须通过 api/ 层）
- ❌ 直接返回 Entity（必须经 Service 处理为 DTO）
- ❌ 在 Controller 中使用 `try-catch` 包裹业务异常（统一全局异常处理）
- ❌ 强制 DTO 转 VO（默认直接返回 DTO，特殊场景再转 VO）

---

## 四、跨模块调用规则（强制执行）

### 4.1 依赖声明规则

**允许的依赖**（在 `pom.xml` 中声明）：
```xml
<!-- hify-module-chat/pom.xml -->
<dependencies>
    <!-- 只能依赖其他模块的 api 包 -->
    <dependency>
        <groupId>com.hify</groupId>
        <artifactId>hify-module-model</artifactId>
    </dependency>
    <dependency>
        <groupId>com.hify</groupId>
        <artifactId>hify-module-agent</artifactId>
    </dependency>

    <!-- 公共工具 -->
    <dependency>
        <groupId>com.hify</groupId>
        <artifactId>hify-common-web</artifactId>
    </dependency>
</dependencies>
```

**禁止的依赖**：
- ❌ 依赖其他模块的 `internal` 包（通过 Maven 插件检查）

### 4.2 调用方式

**正确示例**：chat 模块调用 model 模块
```java
package com.hify.chat.service;

@Service
@RequiredArgsConstructor
public class ChatService {

    // ✅ 正确：注入其他模块的 api 接口
    private final ModelProviderApi modelProviderApi;
    private final AgentApi agentApi;

    public void doChat(Long agentId, String message) {
        // 通过 api 获取数据
        AgentDTO agent = agentApi.getAgent(agentId);
        ModelDTO model = modelProviderApi.getModelById(agent.getModelId());

        // 业务逻辑...
    }
}
```

**错误示例**：
```java
@Service
public class ChatService {

    // ❌ 错误：直接注入其他模块的 service
    @Autowired
    private ModelProviderService modelProviderService;

    // ❌ 错误：注入其他模块的 mapper
    @Autowired
    private AgentMapper agentMapper;
}
```

### 4.3 循环依赖禁止

**如果 A 模块依赖 B，B 模块就不能依赖 A**。解决方法：

```java
// 场景：chat 需要 agent，agent 也需要通知 chat

// ❌ 错误：双向依赖
// hify-module-chat -> hify-module-agent
// hify-module-agent -> hify-module-chat

// ✅ 正确：使用事件解耦
// hify-module-chat 依赖 hify-module-agent（单向）
// agent 模块发布事件，chat 模块监听事件

// Agent 模块发布事件
@Service
public class AgentService {
    @Autowired
    private ApplicationEventPublisher eventPublisher;

    public void updateAgent(AgentDTO agent) {
        // 更新逻辑...
        eventPublisher.publishEvent(new AgentUpdatedEvent(agent.getId()));
    }
}

// Chat 模块监听事件（不直接依赖 agent）
@Component
public class AgentEventListener {
    @EventListener
    public void onAgentUpdated(AgentUpdatedEvent event) {
        // 处理 agent 更新事件
    }
}
```

---

## 五、数据库规范

### 5.1 表前缀（强制）

| 模块 | 表前缀 | 示例表名 |
|------|--------|----------|
| model | `model_` | `model_provider`, `model` |
| agent | `agent_` | `agent`, `agent_tool_bind` |
| chat | `chat_` | `chat_session`, `chat_message` |
| rag | `rag_` | `rag_knowledge_base`, `rag_document` |
| workflow | `workflow_` | `workflow_definition`, `workflow_execution` |
| mcp | `mcp_` | `mcp_server`, `mcp_tool` |

### 5.2 数据库脚本（一期暂不用 Flyway）

**当前方案**：
- 使用 `schema.sql` + `data.sql` 初始化（Spring Boot 原生支持）
- 后续如需迁移，再引入 Flyway

**脚本位置**：
```
hify-server/src/main/resources/
├── schema.sql          # 建表语句（按模块顺序）
└── data.sql            # 初始数据（内置模型提供商等）
```

**表创建顺序**：
```sql
-- schema.sql
-- 1. model 模块（基础）
-- 2. agent 模块（依赖 model）
-- 3. chat 模块（依赖 agent）
-- 4. rag 模块（独立）
-- 5. workflow 模块（依赖 model、rag）
-- 6. mcp 模块（独立）

-- pgvector 扩展（RAG 模块依赖）
CREATE EXTENSION IF NOT EXISTS vector;
```

---

## 六、代码审查检查清单

### 6.1 每次提交前必须检查

```markdown
- [ ] 本修改是否涉及跨模块调用？如果是，是否通过 api/ 层？
- [ ] 新增的 public 方法是否有 JavaDoc 注释？
- [ ] 是否返回了 Entity 给外部？（应该转 DTO/VO）
- [ ] SQL 是否在 XML 中？（禁止注解 SQL）
- [ ] 是否引入了其他模块的 internal 包？
```

### 6.2 代码审查自动化

**Maven Enforcer 插件配置**（在父 pom 中）：

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-enforcer-plugin</artifactId>
    <executions>
        <execution>
            <goals>
                <goal>enforce</goal>
            </goals>
            <configuration>
                <rules>
                    <!-- 禁止依赖其他模块的 internal 包 -->
                    <bannedDependencies>
                        <excludes>
                            <exclude>com.hify:hify-*-internal</exclude>
                        </excludes>
                    </bannedDependencies>
                    <!-- 禁止循环依赖 -->
                    <banCircularDependencies/>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

---

## 七、模块创建模板

新建模块 `hify-module-xxx` 时，复制以下结构：

```
hify-module-xxx/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/hify/xxx/
    │   │   ├── api/
    │   │   │   ├── XxxApi.java
    │   │   │   └── XxxDTO.java
    │   │   ├── service/
    │   │   │   ├── XxxService.java
    │   │   │   └── XxxServiceImpl.java
    │   │   ├── mapper/
    │   │   │   └── XxxMapper.java
    │   │   ├── entity/
    │   │   │   ├── XxxEntity.java
    │   │   │   └── XxxVO.java
    │   │   └── config/
    │   │       └── XxxAutoConfiguration.java
    │   └── resources/
    │       ├── mapper/
    │       └── META-INF/spring.factories
    └── test/java/com/hify/xxx/
```

**pom.xml 模板**：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.hify</groupId>
        <artifactId>hify-parent</artifactId>
        <version>1.0.0</version>
    </parent>

    <artifactId>hify-module-xxx</artifactId>
    <name>Hify Module - XXX</name>

    <dependencies>
        <!-- 公共依赖 -->
        <dependency>
            <groupId>com.hify</groupId>
            <artifactId>hify-common-web</artifactId>
        </dependency>

        <!-- 其他模块 api（按需添加） -->
        <!--
        <dependency>
            <groupId>com.hify</groupId>
            <artifactId>hify-module-model</artifactId>
        </dependency>
        -->
    </dependencies>
</project>
```

---

*规范完*
