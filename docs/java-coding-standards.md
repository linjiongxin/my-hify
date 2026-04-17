# Java 编码规范（阿里巴巴精简版）

> 适用于 Hify 项目的 20 条核心规范，AI 生成代码时必须遵循

---

## 命名规范

| 类型 | 规范 | 示例 |
|------|------|------|
| 类/接口 | UpperCamelCase | `ChatService`, `LlmApiClient` |
| 方法/变量 | lowerCamelCase | `getMessage()`, `userName` |
| 常量 | UPPER_SNAKE_CASE | `MAX_RETRY_COUNT`, `DEFAULT_TIMEOUT` |
| 包名 | 全小写，单数 | `com.hify.chat.service` |
| 布尔变量 | 避免 is 前缀 | `deleted` 而非 `isDeleted`（防序列化问题）|
| 接口实现 | Impl 后缀 | `ChatServiceImpl` 实现 `ChatService` |
| Entity 类名 | 必须与表名对应 | `model_config` → `ModelConfig`，禁止 `Model` 这类泛化命名 |

---

## 异常处理

| 规则 | 要求 |
|------|------|
| 禁止吞异常 | catch 块必须处理或抛出，禁止空 catch |
| 禁止异常控制流程 | 不要用 try-catch 代替 if-else |
| 统一异常返回 | Controller 必须 `@RestControllerAdvice` 统一包装 |
| 必须带上下文 | 日志记录异常时带参数：`log.error("查询失败, userId={}", userId, e)` |
| 资源释放 | 必须用 try-with-resources 或 finally 关闭资源 |
| 自定义异常 | 继承 `RuntimeException`，业务异常不强制捕获 |
| Controller 异常 | **禁止在 Controller 里手动组装 `Result.error()`** | 空值、不存在、无权限等校验必须在 Service 层抛 `BizException`，由 `@RestControllerAdvice` 统一拦截 |

### Controller 层异常处理示例

```java
// 错误：Controller 里手写错误响应
@GetMapping("/{id}")
public Result<UserVO> detail(@PathVariable Long id) {
    UserVO vo = userService.getById(id);
    if (vo == null) {
        return Result.error(404, "数据不存在");  // 禁止！
    }
    return Result.success(vo);
}

// 正确：Service 层抛异常，Controller 只包 success
@GetMapping("/{id}")
public Result<UserVO> detail(@PathVariable Long id) {
    return Result.success(userService.getById(id));
}

// Service 层
public UserVO getById(Long id) {
    UserVO vo = userMapper.selectDetail(id);
    if (vo == null) {
        throw new BizException(ResultCode.DATA_NOT_FOUND, "数据不存在");
    }
    return vo;
}
```

---

## 日志规范

| 规则 | 要求 |
|------|------|
| 框架统一 | SLF4J + Logback，禁止 `System.out.println` |
| 日志级别 | ERROR（故障）/ WARN（警告）/ INFO（关键流程）/ DEBUG（调试）|
| 占位符 | 必须用 `{}` 占位符，禁止字符串拼接 `log.info("a=" + a)` |
| 敏感信息 | 手机号/密码/Token 必须脱敏或打码 |
| 异常日志 | 必须 `log.error("msg", exception)` 带堆栈 |

---

## 并发规范

| 规则 | 要求 |
|------|------|
| 线程池创建 | 必须用 `ThreadPoolExecutor` 手动创建，禁止 `Executors.xxx` |
| 线程命名 | 线程池必须指定命名前缀：`new ThreadFactoryBuilder().setNameFormat("llm-pool-%d").build()` |
| 共享变量 | 并发访问必须加锁（synchronized/Lock）或使用原子类 |
| 锁粒度 | 锁范围最小化，禁止在锁内执行 IO/远程调用 |
| 并发集合 | Map 用 `ConcurrentHashMap`，List 用 `CopyOnWriteArrayList` |
| volatile | 只用于状态标志，不保证原子操作 |

---

## 代码质量

| 规则 | 要求 |
|------|------|
| 魔法值 | 必须定义为常量，禁止硬编码 |
| 空指针 | 入参校验用 `Objects.requireNonNull()`，返回可能为空用 `Optional` |
| 日期时间 | 必须用 Java 8 `java.time`，禁止 `Date`/`Calendar` |
| 字符串拼接 | 循环内必须用 `StringBuilder` |
| 集合容量 | 初始化时预估大小：`new HashMap<>(16)` |
| 数据库 | **禁止在 for 循环内查数据库**，必须批量 |
| 批量更新 | **禁止 for 循环内逐个调用 `updateById`**，必须写批量 SQL 或 `updateBatchById` |

---

## 代码示例

### 正确的日志写法

```java
// 正确
log.info("用户登录成功, userId={}, ip={}", userId, ip);
log.error("查询订单失败, orderId={}", orderId, exception);

// 错误
log.info("用户登录成功, userId=" + userId);  // 字符串拼接
log.error("查询订单失败: " + e.getMessage());  // 丢失堆栈
```

### 正确的线程池创建

```java
// 正确
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    4, 8, 60L, TimeUnit.SECONDS,
    new LinkedBlockingQueue<>(500),
    new ThreadFactoryBuilder().setNameFormat("llm-pool-%d").build(),
    new ThreadPoolExecutor.CallerRunsPolicy()
);

// 错误
ExecutorService executor = Executors.newFixedThreadPool(10);  // 无界队列风险
```

### 正确的空值处理

```java
// 入参校验
public void process(UserDTO user) {
    Objects.requireNonNull(user, "user cannot be null");
    // ...
}

// 返回可能为空
public Optional<UserDTO> findById(Long id) {
    UserEntity entity = userMapper.selectById(id);
    return Optional.ofNullable(entity).map(this::convertToDTO);
}
```

---

## 分层与包结构

### api/ 层边界（跨模块调用唯一入口）

**核心原则：其他模块只能通过 `api/` 包调用本模块能力，禁止直接注入 Service/Mapper/Entity。**

| 规范 | 说明 | 示例 |
|------|------|------|
| 返回类型 | `api/` 方法只能返回 `api/dto/` 下的 DTO，禁止返回 Entity 或内部 VO | `ModelProviderDTO`、`ModelConfigDTO` |
| 禁止直接注入 | 禁止注入其他模块的 `Service`、`Mapper`、`Entity` | `ModelProviderService` ❌ → `ModelProviderApi` ✅ |
| DTO 位置 | 供跨模块调用的 DTO 必须放在 `api/dto/` 包下 | `com.hify.model.api.dto.ModelProviderDTO` |
| 接口位置 | 对外暴露的接口必须放在 `api/` 包下 | `com.hify.model.api.ModelProviderApi` |

### 错误示例

```java
// ❌ 错误：直接注入其他模块的 Service
@Service
public class ChatService {
    @Autowired
    private ModelProviderService modelProviderService;
}

// ❌ 错误：api 层返回内部 Entity
public interface ModelProviderApi {
    ModelProvider getById(Long id);
}
```

### 正确示例

```java
// ✅ 正确：注入 api 接口
@Service
public class ChatService {
    @Autowired
    private ModelProviderApi modelProviderApi;
}

// ✅ 正确：api 层返回专用 DTO
public interface ModelProviderApi {
    ModelProviderDTO getById(Long id);
}
```

---

*规范完*
