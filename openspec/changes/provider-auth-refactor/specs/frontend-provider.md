# 前端页面规范

## 页面位置

`hify-web/src/views/provider/ProviderList.vue`

## 功能

模型提供商的列表展示、新增、编辑、删除。

## 列表字段

| 字段 | 说明 |
|------|------|
| 名称 | `name` |
| 代码 | `code` |
| 协议 | `protocolType`（`openai_compatible` 显示为"OpenAI 兼容"） |
| Base URL | `apiBaseUrl` |
| 鉴权 | `authType`（`BEARER`/`API_KEY`/`NONE`/`CUSTOM`） |
| 状态 | `enabled`（启用/禁用 Tag） |
| 创建时间 | `createdAt` |
| 操作 | 编辑、删除 |

## 表单字段

- **名称**（必填）
- **代码**（必填，如 `openai`、`deepseek`）
- **协议类型**（必填，下拉选择）
- **Base URL**（必填）
- **鉴权类型**（必填，下拉选择）
- **API Key**（密码输入框，`show-password`）

## API 对接

使用 `hify-web/src/api/provider.ts`：

- `getProviderPage(params)`
- `createProvider(data)`
- `updateProvider(id, data)`
- `deleteProvider(id)`

## 路由与菜单

- 路由 `/providers` 指向 `ProviderList.vue`
- 左侧导航栏显示「模型提供商」入口（图标 `OfficeBuilding`），位于「模型管理」下方

## 技术约束

- 使用通用组件 `HifyTable` + `HifyFormDialog`
- 对接真实 API，不再使用 mock 数据
- 删除前通过 `useConfirm` 二次确认
