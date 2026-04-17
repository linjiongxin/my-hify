# 前端模型配置页面规范

## 页面位置

`hify-web/src/views/model/ModelList.vue`

## 功能

模型配置的列表展示、新增、编辑、删除。

## 列表字段

| 字段 | 说明 |
|------|------|
| 提供商 | `providerId`，通过下拉数据映射为 `provider.name` |
| 模型名称 | `name` |
| 模型 ID | `modelId`（如 `gpt-4o`） |
| 默认 | `defaultModel`（Tag 展示） |
| 状态 | `enabled`（启用/禁用 Tag） |
| 创建时间 | `createdAt` |
| 操作 | 编辑、删除 |

## 表单字段

- **提供商**（必填，下拉选择，编辑时禁用）
- **模型名称**（必填）
- **模型 ID**（必填，对应 LLM 的模型标识）
- **最大 Token**（数字输入）
- **上下文窗口**（数字输入）
- **默认模型**（Switch）
- **启用状态**（Switch）

## API 对接

使用 `hify-web/src/api/model.ts`：

- `getModelPage(params)`
- `createModel(data)`
- `updateModel(id, data)`
- `deleteModel(id)`

依赖 `provider.ts`：

- `getProviderPage({ current: 1, size: 999 })` 用于加载下拉框数据并构建 `providerId -> name` 映射

## 路由与菜单

- 路由 `/models` 指向 `ModelList.vue`
- 路由 `/providers` 保留指向 `ProviderList.vue`
- 面包屑 `models` 显示为"模型配置"，`providers` 显示为"模型提供商"

## 技术约束

- 使用通用组件 `HifyTable` + `HifyFormDialog`
- 删除前通过 `useConfirm` 二次确认
- 编辑时 `providerId` 不可修改
