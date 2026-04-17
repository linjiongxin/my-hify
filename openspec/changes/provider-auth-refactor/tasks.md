# Provider 模块数据模型重构 — 任务清单

## 数据库层

- [x] 更新 PostgreSQL schema：`model_provider` 增加 `protocol_type`、`auth_type`、`api_key`、`auth_config`
- [x] 新增 `model_provider_status` 健康状态表
- [x] `model` 表重命名为 `model_config`，扩展 `capabilities`、`context_window`、`input_price_per_1m`、`output_price_per_1m`、`default_model` 等字段
- [x] 更新初始化数据和触发器

## 后端公共组件

- [x] 新增 `JsonbTypeHandler`：支持 PostgreSQL JSONB 与 `Map<String, Object>` 映射

## 后端模型模块（hify-module-model）

- [x] 实体对齐：更新 `ModelProvider`、`ModelConfig`（映射 `model_config`），新增 `ModelProviderStatus`
- [x] 新增 VO：`ModelProviderVO`（含 `healthStatus`）、`ModelConfigVO`
- [x] 新增 DTO：`ModelProviderCreateRequest`、`ModelProviderUpdateRequest`、`ModelConfigCreateRequest`、`ModelConfigUpdateRequest`
- [x] 新增 Mapper 与 XML：`ModelProviderMapper`（含 JOIN 查询）、`ModelProviderStatusMapper`
- [x] 新增 Service：`ModelProviderStatusService`、`ModelConfigService`
- [x] 更新 `ModelProviderServiceImpl`：创建时同步初始化健康状态记录，删除时级联清理
- [x] 新增 `ModelConfigController`：模型 CRUD 接口
- [x] 更新 `ModelProviderController`：对接新的 DTO 和字段
- [x] 重命名 `Model` 实体及相关类为 `ModelConfig` 前缀，保持类名与表名 `model_config` 一致

## 后端网关层

- [x] 更新 `LlmGatewayServiceImpl`：从数据库读取 provider 配置，按 `protocolType` 路由
- [x] 更新 `OpenAiCompatibleProvider`：支持 `BEARER`、`API_KEY`、`NONE`、`CUSTOM` 四种鉴权

## 前端

- [x] 更新 `provider.ts` API 层：补充 `authConfig`、协议类型、鉴权类型等字段
- [x] 重构 `ProviderList.vue`：对接真实 API，替换 mock 数据，支持四种鉴权类型的表单配置
- [x] 新增 `model.ts` API 层：对接 `/model` CRUD 接口
- [x] 新增 `ModelList.vue`：模型配置列表、新增、编辑、删除，含提供商下拉映射
- [x] 修正路由：`/models` 指向模型配置页面，`/providers` 保留为提供商页面
- [x] 更新面包屑映射：区分"模型配置"与"模型提供商"

## 验证

- [x] 编译通过（`mvn clean install -DskipTests`）
- [x] 应用启动后数据库初始化正常
- [x] 前端页面可正常增删改查模型提供商
