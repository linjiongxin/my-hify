# LLM 网关规范

## 核心类

- `LlmGatewayServiceImpl`：网关入口，负责 provider 查找、协议路由、信号量限流
- `LlmProviderFactory`：根据 `protocolType` 获取对应 Provider 实现
- `OpenAiCompatibleProvider`：OpenAI 兼容协议实现

## 调用流程

```
chat(modelId, request)
    ├── 查询 model_config → 获取 providerId
    ├── 查询 model_provider → 获取 protocolType、apiBaseUrl、authType、apiKey、authConfig
    ├── 校验 provider.enabled = true
    ├── protocolType → LlmProviderFactory 获取实现
    ├── 信号量 acquire(providerCode)
    ├── 调用 llmProvider.chat() / chatStream()
    └── 信号量 release()
```

## 认证策略

`OpenAiCompatibleProvider.applyAuthHeaders` 支持四种鉴权：

### BEARER
```
Authorization: Bearer {apiKey}
```

### API_KEY
```
{headerName}: {prefix}{apiKey}
```
- `headerName` 从 `authConfig.headerName` 读取，默认 `api-key`
- `prefix` 从 `authConfig.prefix` 读取，默认空字符串

### NONE
不添加任何认证头。

### CUSTOM
从 `authConfig.headers` 读取键值对，全部注入请求头。

## 运行时覆盖

`LlmChatRequest.extra` 支持运行时覆盖：
- `apiBaseUrl`：覆盖 provider 配置的 Base URL
- `apiKey`：覆盖 provider 配置的 API Key
- `protocolType`：覆盖 provider 配置的协议类型

## 超时策略

- 连接超时：5s
- 非流式读取超时：60s
- 流式读取超时：120s
- 连接池：200 连接，5 分钟保活
