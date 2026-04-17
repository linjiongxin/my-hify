# 后端 API 规范

## 模型提供商 API

Base path: `/model-provider`

### 分页列表

```
GET /model-provider/page?pageNum={n}&pageSize={n}
```

**返回：** `PageResult<ModelProviderVO>`

### 详情

```
GET /model-provider/{id}
```

**返回：** `ModelProviderVO`（含 `healthStatus`）

### 创建

```
POST /model-provider
```

**Body：** `ModelProviderCreateRequest`
- `name`: string (required)
- `code`: string (required)
- `protocolType`: string (required)
- `apiBaseUrl`: string (required)
- `authType`: string (required)
- `apiKey`: string
- `authConfig`: Map<string, object>
- `enabled`: boolean
- `sortOrder`: int

**行为：** 保存 provider 后同步初始化 `model_provider_status`，`healthStatus = unknown`。

### 更新

```
PUT /model-provider/{id}
```

**Body：** `ModelProviderUpdateRequest`（字段同创建，不含 `code`）

### 删除

```
DELETE /model-provider/{id}
```

**行为：** 级联删除 `model_provider_status`（数据库 `ON DELETE CASCADE` + 代码兜底）。

---

## 模型 API

Base path: `/model`

### 分页列表

```
GET /model/page?pageNum={n}&pageSize={n}
```

**返回：** `PageResult<ModelConfigVO>`

### 详情

```
GET /model/{id}
```

### 创建

```
POST /model
```

**Body：** `ModelConfigCreateRequest`
- `providerId`: long (required)
- `name`: string (required)
- `modelId`: string (required)
- `maxTokens`: int
- `contextWindow`: int
- `capabilities`: Map<string, object>
- `inputPricePer1m`: decimal
- `outputPricePer1m`: decimal
- `defaultModel`: boolean
- `enabled`: boolean
- `sortOrder`: int

**行为：** 若 `defaultModel = true`，取消该 provider 下其他默认模型。

### 更新

```
PUT /model/{id}
```

**Body：** `ModelConfigUpdateRequest`

**行为：** 若从非默认改为默认，取消该 provider 下其他默认模型。

### 删除

```
DELETE /model/{id}
```

---

## 关键实现约束

- `ModelProviderServiceImpl.createProvider` 必须在同一事务中初始化 `ModelProviderStatus`
- `ModelServiceImpl.createModel` / `updateModel` 必须处理 `defaultModel` 的唯一性
- 复杂查询必须写 XML，禁止 `@Select` 注解 SQL
- api/ 层返回 DTO/VO，禁止返回 Entity
