## 1. 数据库与实体

- [ ] 1.1 更新 `hify-schema.sql` 中的 RAG 表（knowledge_base, document, document_chunk, agent_knowledge_base）
- [ ] 1.2 创建 `KnowledgeBase` Entity（对应 knowledge_base 表）
- [ ] 1.3 创建 `Document` Entity（对应 document 表）
- [ ] 1.4 更新 `DocumentChunk` Entity（适配新表结构，向量维度 1024）
- [ ] 1.5 创建 `AgentKnowledgeBase` Entity（对应 agent_knowledge_base 表）

## 2. API 层（api/）

- [ ] 2.1 定义 `KnowledgeBaseApi` 接口（CRUD）
- [ ] 2.2 定义 `DocumentApi` 接口（上传/列表/删除）
- [ ] 2.3 定义 `AgentKnowledgeBaseApi` 接口（Agent 绑定知识库）

## 3. Service 层（service/）

- [ ] 3.1 实现 `KnowledgeBaseService`（知识库 CRUD）
- [ ] 3.2 实现 `DocumentService`（文档上传/解析/状态管理）
- [ ] 3.3 实现 `AgentKnowledgeBaseService`（Agent 绑定管理）

## 4. 文档解析与分块（core/）

- [ ] 4.1 实现 `DocumentParser` 接口（TXT/Markdown 解析器）
- [ ] 4.2 实现 `RecursiveChunker`（递归分块算法）
- [ ] 4.3 实现 `EmbeddingService`（调用阿里 embedding API）

## 5. RAG 检索（service/）

- [ ] 5.1 实现 `RagSearchService`（向量检索 + 阈值过滤）
- [ ] 5.2 实现 `RagContextBuilder`（将 chunks 拼接为 LLM 上下文）

## 6. Agent 集成

- [ ] 6.1 修改 `ChatService`，在调用 LLM 前注入 RAG 上下文
- [ ] 6.2 修改 `AgentService`，支持查询 Agent 绑定的知识库

## 7. Mapper 层（mapper/）

- [ ] 7.1 创建 `KnowledgeBaseMapper`
- [ ] 7.2 创建 `DocumentMapper`
- [ ] 7.3 创建 `AgentKnowledgeBaseMapper`
- [ ] 7.4 更新 `DocumentChunkMapper`（适配新的表结构）

## 8. 前端页面

- [ ] 8.1 知识库管理页面（列表/创建/编辑/删除）
- [ ] 8.2 文档管理页面（上传/列表/删除）
- [ ] 8.3 Agent 配置页面（绑定知识库，设置 top_k / similarity_threshold）

## 9. 集成测试

- [ ] 9.1 文档解析 + 分块测试
- [ ] 9.2 向量化 + 检索测试
- [ ] 9.3 Agent RAG 上下文注入测试