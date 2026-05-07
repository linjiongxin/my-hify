---
name: rag-knowledge-base
description: RAG 知识库模块 - 私有知识管理、文档上传、递归分块、Agent 集成
status: proposed
created: 2026-04-29
---

# RAG 知识库模块设计

## Context

项目已有 `hify-module-rag` 模块的最小 Demo，但尚未集成到 Agent 对话流程中。需要扩展为完整的知识库管理功能。

### 技术选型

| 决策 | 选项 | 原因 |
|------|------|------|
| Embedding 模型 | 阿里 text-embedding-v2 (1024 维) | 成本低、效果好、国内访问稳定 |
| 分块策略 | 递归分块（段落优先，长段落再拆小） | 保留语义完整性 |
| 向量检索 | 纯向量检索 + 阈值 0.7 + Top-5 | 简化方案，满足当前需求 |
| 文档格式 | TXT、Markdown（已有 Demo 支持） | 够用，PDF 后续扩展 |

### 数据库表设计

| 表名 | 说明 |
|------|------|
| `knowledge_base` | 知识库配置（embedding_model、chunk_size、chunk_overlap） |
| `document` | 文档（file_name、file_type、status、parsed_content） |
| `document_chunk` | 分块 + 向量（VECTOR(1024)） |
| `agent_knowledge_base` | Agent × 知识库绑定（top_k、similarity_threshold） |

---

## Goals

1. **知识库管理**：管理员可创建/编辑/删除知识库，配置 embedding 模型和分块参数
2. **文档上传**：支持 TXT/Markdown 上传，自动解析并分块
3. **递归分块**：按段落分块，长段落（>512 tokens）再递归拆分为更小的语义单元
4. **Agent 集成**：Agent 绑定知识库后，对话时自动检索相关 chunks 作为上下文
5. **前端页面**：知识库列表、文档管理、Agent 配置页面

---

## Non-Goals

- PDF/Word 文档解析（后期扩展）
- Reranking（重排序）
- 混合检索（BM25 + 向量）
- 多租户隔离

---

## Decisions

### 1. 知识库 API 设计

```yaml
POST   /api/rag/knowledge-bases          # 创建知识库
GET    /api/rag/knowledge-bases          # 列表
GET    /api/rag/knowledge-bases/{id}     # 详情
PUT    /api/rag/knowledge-bases/{id}      # 更新
DELETE /api/rag/knowledge-bases/{id}      # 删除

POST   /api/rag/knowledge-bases/{kbId}/documents   # 上传文档
GET    /api/rag/knowledge-bases/{kbId}/documents   # 文档列表
GET    /api/rag/documents/{id}           # 文档详情
DELETE /api/rag/documents/{id}           # 删除文档
```

### 2. 文档处理流程

```
上传文件 → 解析（TXT/Markdown 纯文本提取）→ 递归分块 → 向量化 → 存储
           ↓
        async 处理，status: pending → processing → completed/failed
```

### 3. 递归分块算法

```
function recursiveChunk(text, maxTokens=512, overlap=50):
    paragraphs = splitByDoubleNewline(text)  # 按空行分段
    
    chunks = []
    for paragraph in paragraphs:
        if tokens(paragraph) <= maxTokens:
            chunks.add(paragraph)
        else:
            # 长段落按句子拆分，再合并到 maxTokens
            sentences = splitBySentence(paragraph)
            currentChunk = []
            for sentence in sentences:
                if tokens(concat(currentChunk, sentence)) > maxTokens:
                    chunks.add(concat(currentChunk))
                    currentChunk = [sentence]  # overlap 通过上一个块的尾部句子实现
                else:
                    currentChunk.add(sentence)
            if currentChunk:
                chunks.add(concat(currentChunk))
    
    return chunks
```

### 4. Agent RAG 集成

Agent 对话时，在调用 LLM 前：
1. 查询 `agent_knowledge_base` 获取绑定的知识库
2. 对用户问题做 embedding
3. 向量检索 `document_chunk`，使用配置的 top_k 和 similarity_threshold
4. 将检索到的 chunks 内容拼接为 system prompt 前缀

---

## Risks

| 风险 | 影响 | 缓解 |
|------|------|------|
| 长文本分块效果差 | 检索召回质量下降 | 后期可调整为父子分块 |
| 向量检索慢 | 用户等待时间长 | 适当调整 ivfflat 的 lists 参数 |
| 文档解析失败 | 用户上传的文档无法使用 | 增加错误信息记录，支持重试 |

---

## Trade-offs

- **简洁 vs 扩展性**：当前只用纯向量检索，适合数据量 < 100 万的场景
- **同步 vs 异步**：文档处理用异步，避免上传阻塞；分块和向量化在后台线程处理
- **嵌入 Agent vs 独立 RAG API**：集成到 Agent 配置中，简单但不够灵活；后续可拆分为独立 RAG 工具调用