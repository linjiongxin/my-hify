# {{CONTROLLER_NAME}}

> 由 api-generating Skill 自动生成

## 基本信息

| 属性 | 值 |
|------|-----|
| **Controller** | `{{CONTROLLER_CLASS}}` |
| **基础路径** | `{{BASE_PATH}}` |
| **源文件** | `{{SOURCE_FILE}}` |

---

## 接口列表

{{#each ENDPOINTS}}

### {{METHOD}} `{{FULL_PATH}}`

{{#if AUTH}}🔒 需要认证{{/if}}
{{#if PERMISSION}}🔑 权限要求: `{{PERMISSION}}`{{/if}}

**方法名**: `{{METHOD_NAME}}`

**接口说明**: {{DESCRIPTION}}

{{#if PATH_PARAMS}}
**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
{{#each PATH_PARAMS}}
| `{{name}}` | {{type}} | {{required}} | {{description}} |
{{/each}}
{{/if}}

{{#if QUERY_PARAMS}}
**查询参数**:

| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
{{#each QUERY_PARAMS}}
| `{{name}}` | {{type}} | {{required}} | {{default}} | {{description}} |
{{/each}}
{{/if}}

{{#if REQUEST_BODY}}
**请求体**:

```json
{{REQUEST_BODY_EXAMPLE}}
```
{{/if}}

**响应类型**: `{{RESPONSE_TYPE}}`

{{#if RESPONSE_EXAMPLE}}
**响应示例**:

```json
{{RESPONSE_EXAMPLE}}
```
{{/if}}

---

{{/each}}

## 通用错误码

| 状态码 | 错误码 | 说明 |
|--------|--------|------|
| 400 | BAD_REQUEST | 请求参数错误 |
| 401 | UNAUTHORIZED | 未认证或认证过期 |
| 403 | FORBIDDEN | 无权限访问 |
| 404 | NOT_FOUND | 资源不存在 |
| 500 | INTERNAL_ERROR | 服务器内部错误 |
