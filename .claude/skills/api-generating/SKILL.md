---
name: api-generating
description: 从 Spring Boot 项目的 Controller 文件生成 API 接口文档。
allowed-tools: [Read, Grep, Glob, Write, Bash(python *)]
---

# Spring Boot API 文档生成 Skill

## 工作流程 — 必须遵循

**重要**: 你必须按顺序执行以下步骤。不要跳过或替代任何步骤。

### 步骤 1: Controller 发现（路由发现）

**必须使用 Python 脚本扫描 Controller：**

```bash
python3 .claude/skills/api-generating/scripts/detect-controllers.py .
```

不要用 Grep 手动搜索 Controller —— 脚本会处理以下复杂情况：
- 类级别的 `@RequestMapping` 前缀
- 方法级别的各种 HTTP 注解（`@GetMapping`, `@PostMapping` 等）
- 路径变量（`@PathVariable`）和查询参数（`@RequestParam`）
- 请求体（`@RequestBody`）和响应类型
- Spring Security 权限注解（`@PreAuthorize`, `@Secured`）

### 步骤 2: 接口分析（路由分析）

对脚本发现的每个 Controller 和接口：

1. **读取 Controller 源文件**
2. **提取信息：**
   - HTTP 方法（GET/POST/PUT/DELETE 等）
   - 完整路径（类前缀 + 方法路径）
   - 参数列表（名称、类型、必填、位置）
   - 请求体 Schema（如果是 POST/PUT）
   - 响应类型和示例
   - 权限注解（`@PreAuthorize` 等）
   - 接口说明（JavaDoc/KDoc 注释）

### 步骤 3: 文档生成

使用 `templates/spring-api-doc.md` 模板生成文档。

**输出规则：**
- 每个 Controller 一个 Markdown 文件（如 `docs/api/UserController.md`）
- 包含请求/响应示例
- 需要认证的接口标记 🔒
- 需要特定权限的接口标记 🔑

## 参考文件

- Controller 扫描脚本：`scripts/detect-controllers.py`
- 文档模板：`templates/spring-api-doc.md`

## 质量检查清单

完成前，验证：
- [ ] 脚本输出的所有接口都已文档化
- [ ] 请求/响应 Schema 与实际代码一致
- [ ] 认证和权限要求已标记
- [ ] 示例是有效的 JSON
- [ ] JavaDoc 注释已提取并显示
