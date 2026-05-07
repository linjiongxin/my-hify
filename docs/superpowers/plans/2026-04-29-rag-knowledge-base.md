# RAG 知识库模块实现计划

> **面向执行者：** 本计划由 OpenSpec 规格驱动。执行前请先阅读 `openspec/changes/rag-knowledge-base/design.md` 了解上下文。
> **执行方式：** `superpowers:subagent-driven-development`（推荐）或 `superpowers:executing-plans`
> **步骤标记：** `- [ ]` 未执行，`- [x]` 已完成

**目标：** 实现完整的 RAG 知识库模块，支持私有知识管理、文档上传、递归分块、Agent 集成

**架构：**
- 后端：Spring Boot + MyBatis-Plus + pgvector
- 前端：Vue 3 + Element Plus
- 文档处理：异步任务，状态流转 pending → processing → completed/failed

**技术栈：** Java 17, Spring Boot 3.x, MyBatis-Plus, PostgreSQL pgvector, Vue 3

**范围护栏：**
- 目标：知识库 CRUD、文档上传（TXT/Markdown）、递归分块、Agent RAG 集成、前端页面
- 非目标：PDF/Word 解析、Reranking、混合检索、多租户隔离

---

## Task 1: 更新 SQL 表结构

**类型：** 结构变更

**文件：**
- 修改：`docs/sql/init/hify-schema.sql`

**依赖：** 无

**描述：** 将旧的 `rag_chunk` 表替换为新的 `knowledge_base`、`document`、`document_chunk`、`agent_knowledge_base` 四张表（已在 design 阶段完成，本步骤确认）

- [ ] **Step 1: 确认 SQL 已更新**

检查 `docs/sql/init/hify-schema.sql` 第 246-310 行是否包含新的 RAG 表结构：
- `knowledge_base`（VARCHAR(128) name, embedding_model, chunk_size）
- `document`（parsed_content, total_chunks, error_message）
- `document_chunk`（VECTOR(1024), meta_json GIN 索引）
- `agent_knowledge_base`（agent × kb 绑定）

预期：已完成（design 阶段已更新）

- [ ] **Step 2: 提交（如有修改）**

```bash
git add docs/sql/init/hify-schema.sql
git commit -m "feat(rag): update RAG table schema to v2"
```

---

## Task 2: 创建 KnowledgeBase Entity

**类型：** 结构变更

**文件：**
- 创建：`hify-module-rag/src/main/java/com/hify/rag/entity/KnowledgeBase.java`

**依赖：** Task 1

**描述：** 创建知识库实体类，对应 `knowledge_base` 表

- [ ] **Step 1: 编写 Entity**

```java
package com.hify.rag.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hify.common.web.entity.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("knowledge_base")
public class KnowledgeBase extends BaseEntity {

    private String name;
    private String description;
    private String embeddingModel;
    private Integer chunkSize;
    private Integer chunkOverlap;
    private Boolean enabled;
}
```

- [ ] **Step 2: 提交**

```bash
git add hify-module-rag/src/main/java/com/hify/rag/entity/KnowledgeBase.java
git commit -m "feat(rag): add KnowledgeBase entity"
```

---

## Task 3: 创建 Document Entity

**类型：** 结构变更

**文件：**
- 创建：`hify-module-rag/src/main/java/com/hify/rag/entity/Document.java`

**依赖：** Task 2

**描述：** 创建文档实体类，对应 `document` 表

- [ ] **Step 1: 编写 Entity**

```java
package com.hify.rag.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hify.common.web.entity.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("document")
public class Document extends BaseEntity {

    private Long kbId;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private String status; // pending, processing, completed, failed
    private String parsedContent;
    private Integer totalChunks;
    private String errorMessage;
}
```

- [ ] **Step 2: 提交**

```bash
git add hify-module-rag/src/main/java/com/hify/rag/entity/Document.java
git commit -m "feat(rag): add Document entity"
```

---

## Task 4: 更新 DocumentChunk Entity

**类型：** 结构变更

**文件：**
- 修改：`hify-module-rag/src/main/java/com/hify/rag/entity/DocumentChunk.java`

**依赖：** Task 1

**描述：** 更新现有 DocumentChunk Entity，适配 VECTOR(1024) 和新的字段

- [ ] **Step 1: 更新 Entity**

```java
package com.hify.rag.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hify.common.web.entity.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("document_chunk")
public class DocumentChunk extends BaseEntity {

    private Long kbId;
    private Long documentId;
    private String content;
    private String embedding; // VECTOR(1024) as String
    private Integer chunkIndex;
    private String metaJson; // JSONB as String
    private Boolean enabled;
}
```

- [ ] **Step 2: 提交**

```bash
git add hify-module-rag/src/main/java/com/hify/rag/entity/DocumentChunk.java
git commit -m "feat(rag): update DocumentChunk entity for VECTOR(1024)"
```

---

## Task 5: 创建 AgentKnowledgeBase Entity

**类型：** 结构变更

**文件：**
- 创建：`hify-module-rag/src/main/java/com/hify/rag/entity/AgentKnowledgeBase.java`

**依赖：** Task 2

**描述：** 创建 Agent × 知识库绑定实体

- [ ] **Step 1: 编写 Entity**

```java
package com.hify.rag.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hify.common.web.entity.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("agent_knowledge_base")
public class AgentKnowledgeBase extends BaseEntity {

    private Long agentId;
    private Long kbId;
    private Integer topK;
    private BigDecimal similarityThreshold;
    private Boolean enabled;
}
```

- [ ] **Step 2: 提交**

```bash
git add hify-module-rag/src/main/java/com/hify/rag/entity/AgentKnowledgeBase.java
git commit -m "feat(rag): add AgentKnowledgeBase entity"
```

---

## Task 6: 定义 KnowledgeBaseApi 接口

**类型：** 结构变更

**文件：**
- 创建：`hify-module-rag/src/main/java/com/hify/rag/api/KnowledgeBaseApi.java`
- 创建：`hify-module-rag/src/main/java/com/hify/rag/dto/KnowledgeBaseCreateDTO.java`
- 创建：`hify-module-rag/src/main/java/com/hify/rag/dto/KnowledgeBaseUpdateDTO.java`

**依赖：** Task 2

**描述：** 定义知识库 CRUD API 接口和 DTO

- [ ] **Step 1: 编写 DTO**

```java
// KnowledgeBaseCreateDTO
public class KnowledgeBaseCreateDTO {
    @NotBlank
    private String name;
    private String description;
    private String embeddingModel;
    private Integer chunkSize;
    private Integer chunkOverlap;
}

// KnowledgeBaseUpdateDTO
public class KnowledgeBaseUpdateDTO {
    private String name;
    private String description;
    private String embeddingModel;
    private Integer chunkSize;
    private Integer chunkOverlap;
    private Boolean enabled;
}
```

- [ ] **Step 2: 编写 API 接口**

```java
public interface KnowledgeBaseApi {

    Long create(KnowledgeBaseCreateDTO dto);

    void update(Long id, KnowledgeBaseUpdateDTO dto);

    void delete(Long id);

    KnowledgeBaseDTO getById(Long id);

    PageResult<KnowledgeBaseDTO> list(KnowledgeBaseQueryDTO query);
}
```

- [ ] **Step 3: 提交**

```bash
git add hify-module-rag/src/main/java/com/hify/rag/api/KnowledgeBaseApi.java \
      hify-module-rag/src/main/java/com/hify/rag/dto/KnowledgeBaseCreateDTO.java \
      hify-module-rag/src/main/java/com/hify/rag/dto/KnowledgeBaseUpdateDTO.java
git commit -m "feat(rag): define KnowledgeBaseApi and DTOs"
```

---

## Task 7: 定义 DocumentApi 接口

**类型：** 结构变更

**文件：**
- 创建：`hify-module-rag/src/main/java/com/hify/rag/api/DocumentApi.java`
- 创建：`hify-module-rag/src/main/java/com/hify/rag/dto/DocumentUploadDTO.java`

**依赖：** Task 3

**描述：** 定义文档上传/管理 API

- [ ] **Step 1: 编写 DTO 和 API**

```java
// DocumentUploadDTO (用于 MultipartFile 上传)
public class DocumentUploadDTO {
    private Long kbId;
    private MultipartFile file;
}

// DocumentApi
public interface DocumentApi {

    Long uploadDocument(Long kbId, MultipartFile file);

    DocumentDTO getById(Long id);

    PageResult<DocumentDTO> listByKbId(Long kbId, PageQueryDTO query);

    void delete(Long id);

    void retryProcess(Long id);
}
```

- [ ] **Step 2: 提交**

```bash
git add hify-module-rag/src/main/java/com/hify/rag/api/DocumentApi.java \
      hify-module-rag/src/main/java/com/hify/rag/dto/DocumentUploadDTO.java
git commit -m "feat(rag): define DocumentApi and DTOs"
```

---

## Task 8: 定义 AgentKnowledgeBaseApi 接口

**类型：** 结构变更

**文件：**
- 创建：`hify-module-rag/src/main/java/com/hify/rag/api/AgentKnowledgeBaseApi.java`
- 创建：`hify-module-rag/src/main/java/com/hify/rag/dto/AgentKbBindingDTO.java`

**依赖：** Task 5

**描述：** 定义 Agent 绑定知识库的 API

- [ ] **Step 1: 编写 DTO 和 API**

```java
public class AgentKbBindingDTO {
    private Long agentId;
    private Long kbId;
    private Integer topK; // default 5
    private BigDecimal similarityThreshold; // default 0.7
}

public interface AgentKnowledgeBaseApi {

    void bind(AgentKbBindingDTO dto);

    void unbind(Long agentId, Long kbId);

    List<AgentKnowledgeBaseDTO> getByAgentId(Long agentId);

    void updateBinding(Long agentId, Long kbId, Integer topK, BigDecimal similarityThreshold);
}
```

- [ ] **Step 2: 提交**

```bash
git add hify-module-rag/src/main/java/com/hify/rag/api/AgentKnowledgeBaseApi.java \
      hify-module-rag/src/main/java/com/hify/rag/dto/AgentKbBindingDTO.java
git commit -m "feat(rag): define AgentKnowledgeBaseApi and DTOs"
```

---

## Task 9: 创建 KnowledgeBaseMapper

**类型：** 集成测试 TDD

**文件：**
- 创建：`hify-module-rag/src/main/java/com/hify/rag/mapper/KnowledgeBaseMapper.java`
- 创建：`hify-module-rag/src/main/resources/mapper/KnowledgeBaseMapper.xml`
- 创建：`hify-module-rag/src/test/java/com/hify/rag/mapper/KnowledgeBaseMapperIT.java`
- 更新：`src/test/resources/sql/rag-test-data.sql`

**依赖：** Task 2

**描述：** 创建知识库 Mapper，含基础 CRUD 和分页查询

- [ ] **Step 1: 编写测试数据 SQL**

```sql
-- rag-test-data.sql
TRUNCATE TABLE knowledge_base CASCADE;
INSERT INTO knowledge_base (id, name, embedding_model, chunk_size, chunk_overlap, enabled, created_at, is_deleted)
VALUES (1, '测试知识库', 'text-embedding-v2', 512, 50, true, CURRENT_TIMESTAMP, false);
```

- [ ] **Step 2: 编写集成测试**

```java
@SpringBootTest(classes = HifyApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Transactional
@Sql(scripts = "/sql/rag-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class KnowledgeBaseMapperIT {

    @Autowired
    private KnowledgeBaseMapper mapper;

    @Test
    void shouldSelectById_whenExists() {
        KnowledgeBase kb = mapper.selectById(1L);
        assertThat(kb).isNotNull();
        assertThat(kb.getName()).isEqualTo("测试知识库");
    }
}
```

- [ ] **Step 3: 编写 Mapper XML**

```xml
<mapper namespace="com.hify.rag.mapper.KnowledgeBaseMapper">
    <select id="selectPage" resultType="KnowledgeBase">
        SELECT * FROM knowledge_base
        WHERE is_deleted = false AND enabled = true
        ORDER BY created_at DESC
    </select>
</mapper>
```

- [ ] **Step 4: 运行测试确认通过**

运行：`mvn test -Dtest=KnowledgeBaseMapperIT -pl hify-module-rag`
预期：PASS

- [ ] **Step 5: 提交**

```bash
git add hify-module-rag/src/main/java/com/hify/rag/mapper/KnowledgeBaseMapper.java \
      hify-module-rag/src/main/resources/mapper/KnowledgeBaseMapper.xml \
      hify-module-rag/src/test/java/com/hify/rag/mapper/KnowledgeBaseMapperIT.java
git commit -m "feat(rag): add KnowledgeBaseMapper with CRUD"
```

---

## Task 10: 创建 KnowledgeBaseService

**类型：** 单元测试 TDD

**文件：**
- 创建：`hify-module-rag/src/main/java/com/hify/rag/service/KnowledgeBaseService.java`
- 创建：`hify-module-rag/src/test/java/com/hify/rag/service/KnowledgeBaseServiceTest.java`

**依赖：** Task 6, Task 9

**描述：** 实现知识库 CRUD Service

- [ ] **Step 1: 编写单元测试**

```java
class KnowledgeBaseServiceTest {

    @Mock
    private KnowledgeBaseMapper mapper;

    @InjectMocks
    private KnowledgeBaseService service;

    @Test
    void shouldCreate_whenValidDto() {
        // Given
        KnowledgeBaseCreateDTO dto = new KnowledgeBaseCreateDTO();
        dto.setName("测试知识库");

        // When
        Long id = service.create(dto);

        // Then
        assertThat(id).isNotNull();
        verify(mapper).insert(any(KnowledgeBase.class));
    }
}
```

- [ ] **Step 2: 编写最小实现**

```java
@Service
public class KnowledgeBaseService implements KnowledgeBaseApi {

    @Autowired
    private KnowledgeBaseMapper mapper;

    @Override
    public Long create(KnowledgeBaseCreateDTO dto) {
        KnowledgeBase kb = new KnowledgeBase();
        BeanUtils.copyProperties(dto, kb);
        mapper.insert(kb);
        return kb.getId();
    }

    // ... 其他方法
}
```

- [ ] **Step 3: 运行测试确认通过**

运行：`mvn test -Dtest=KnowledgeBaseServiceTest -pl hify-module-rag`
预期：PASS

- [ ] **Step 4: 提交**

```bash
git add hify-module-rag/src/main/java/com/hify/rag/service/KnowledgeBaseService.java \
      hify-module-rag/src/test/java/com/hify/rag/service/KnowledgeBaseServiceTest.java
git commit -m "feat(rag): implement KnowledgeBaseService"
```

---

## Task 11: 实现 DocumentParser 接口（TXT/Markdown）

**类型：** 单元测试 TDD

**文件：**
- 创建：`hify-module-rag/src/main/java/com/hify/rag/core/DocumentParser.java`
- 创建：`hify-module-rag/src/main/java/com/hify/rag/core/impl/TxtDocumentParser.java`
- 创建：`hify-module-rag/src/main/java/com/hify/rag/core/impl/MarkdownDocumentParser.java`
- 创建：`hify-module-rag/src/test/java/com/hify/rag/core/DocumentParserTest.java`

**依赖：** Task 7

**描述：** 实现文档解析器，从 TXT/Markdown 提取纯文本

- [ ] **Step 1: 编写接口和实现**

```java
public interface DocumentParser {
    String parse(InputStream input);
    boolean supports(String fileType);
}

@Component
public class TxtDocumentParser implements DocumentParser {
    @Override
    public String parse(InputStream input) {
        return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }

    @Override
    public boolean supports(String fileType) {
        return "txt".equalsIgnoreCase(fileType);
    }
}

@Component
public class MarkdownDocumentParser implements DocumentParser {
    @Override
    public String parse(InputStream input) {
        String content = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        // 移除 markdown 标记，保留纯文本
        return content.replaceAll(#+*\s*, "")
                      .replaceAll("!\[.*\]\(.*\)", "")
                      .replaceAll("\[([^\]]+)\]\([^\)]+\)", "$1");
    }

    @Override
    public boolean supports(String fileType) {
        return "md".equalsIgnoreCase(fileType);
    }
}
```

- [ ] **Step 2: 编写单元测试**

```java
class DocumentParserTest {

    @Test
    void shouldParseTxtFile() {
        var parser = new TxtDocumentParser();
        String content = parser.parse(new ByteArrayInputStream("Hello World".getBytes()));
        assertThat(content).isEqualTo("Hello World");
    }

    @Test
    void shouldParseMarkdownFile() {
        var parser = new MarkdownDocumentParser();
        String content = parser.parse(new ByteArrayInputStream("# Title\n[link](url)".getBytes()));
        assertThat(content).contains("Title");
        assertThat(content).doesNotContain("[link](url)");
    }

    @Test
    void shouldReturnTrue_whenSupportsTxt() {
        assertThat(new TxtDocumentParser().supports("txt")).isTrue();
        assertThat(new TxtDocumentParser().supports("TXT")).isTrue();
    }
}
```

- [ ] **Step 3: 运行测试确认通过**

运行：`mvn test -Dtest=DocumentParserTest -pl hify-module-rag`
预期：PASS

- [ ] **Step 4: 提交**

```bash
git add hify-module-rag/src/main/java/com/hify/rag/core/DocumentParser.java \
      hify-module-rag/src/main/java/com/hify/rag/core/impl/TxtDocumentParser.java \
      hify-module-rag/src/main/java/com/hify/rag/core/impl/MarkdownDocumentParser.java \
      hify-module-rag/src/test/java/com/hify/rag/core/DocumentParserTest.java
git commit -m "feat(rag): implement DocumentParser for TXT and Markdown"
```

---

## Task 12: 实现 RecursiveChunker（递归分块）

**类型：** 单元测试 TDD

**文件：**
- 创建：`hify-module-rag/src/main/java/com/hify/rag/core/RecursiveChunker.java`
- 创建：`hify-module-rag/src/test/java/com/hify/rag/core/RecursiveChunkerTest.java`

**依赖：** Task 11

**描述：** 实现递归分块算法：先按段落拆分，长段落按句子再合并

- [ ] **Step 1: 编写单元测试**

```java
class RecursiveChunkerTest {

    private RecursiveChunker chunker = new RecursiveChunker(512, 50);

    @Test
    void shouldChunkByParagraph_whenShortParagraphs() {
        String text = "第一段内容。\n\n第二段内容。";
        List<String> chunks = chunker.chunk(text);

        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0)).isEqualTo("第一段内容。");
    }

    @Test
    void shouldSplitLongParagraph_whenExceedsMaxTokens() {
        // 创建一个超过 512 tokens 的长段落
        String longText = "句子A。句子B。句子C。".repeat(50);
        List<String> chunks = chunker.chunk(longText);

        assertThat(chunks).isNotEmpty();
        // 每个 chunk 应该不超过 512 tokens
        for (String chunk : chunks) {
            assertThat(estimateTokens(chunk)).isLessThanOrEqualTo(512);
        }
    }
}
```

- [ ] **Step 2: 编写最小实现**

```java
public class RecursiveChunker {

    private final int maxTokens;
    private final int overlapTokens;

    public RecursiveChunker(int maxTokens, int overlapTokens) {
        this.maxTokens = maxTokens;
        this.overlapTokens = overlapTokens;
    }

    public List<String> chunk(String text) {
        List<String> result = new ArrayList<>();
        String[] paragraphs = text.split("\n\n");

        for (String paragraph : paragraphs) {
            if (paragraph.trim().isEmpty()) continue;

            if (estimateTokens(paragraph) <= maxTokens) {
                result.add(paragraph);
            } else {
                result.addAll(splitLongParagraph(paragraph));
            }
        }
        return result;
    }

    private List<String> splitLongParagraph(String paragraph) {
        List<String> chunks = new ArrayList<>();
        List<String> sentences = splitSentences(paragraph);
        List<String> currentChunk = new ArrayList<>();

        for (String sentence : sentences) {
            if (estimateTokens(String.join("", currentChunk) + sentence) > maxTokens) {
                chunks.add(String.join("", currentChunk));
                currentChunk = new ArrayList<>();
            }
            currentChunk.add(sentence);
        }
        if (!currentChunk.isEmpty()) {
            chunks.add(String.join("", currentChunk));
        }
        return chunks;
    }

    private List<String> splitSentences(String text) {
        // 按句末标点拆分
        return Arrays.asList(text.split("(?<=[。！？.!?])"));
    }

    private int estimateTokens(String text) {
        // 粗略估算：中文按字符数，英文按单词数 * 1.5
        return (int) (text.chars().filter(c -> c > 127).count()
                + text.split("\\s+").length * 1.5);
    }
}
```

- [ ] **Step 3: 运行测试确认通过**

运行：`mvn test -Dtest=RecursiveChunkerTest -pl hify-module-rag`
预期：PASS

- [ ] **Step 4: 提交**

```bash
git add hify-module-rag/src/main/java/com/hify/rag/core/RecursiveChunker.java \
      hify-module-rag/src/test/java/com/hify/rag/core/RecursiveChunkerTest.java
git commit -m "feat(rag): implement RecursiveChunker algorithm"
```

---

## Task 13: 实现 EmbeddingService（调用阿里 API）

**类型：** 单元测试 TDD

**文件：**
- 创建：`hify-module-rag/src/main/java/com/hify/rag/core/EmbeddingService.java`
- 创建：`hify-module-rag/src/main/java/com/hify/rag/core/impl/AliEmbeddingService.java`
- 创建：`hify-module-rag/src/test/java/com/hify/rag/core/EmbeddingServiceTest.java`

**依赖：** Task 12

**描述：** 实现 Embedding 服务，调用阿里 text-embedding-v2 API

- [ ] **Step 1: 编写接口和实现**

```java
public interface EmbeddingService {
    float[] embed(String text);
    List<float[]> batchEmbed(List<String> texts);
}

@Component
public class AliEmbeddingService implements EmbeddingService {

    @Value("${hify.embedding.ali.api-key}")
    private String apiKey;

    @Value("${hify.embedding.ali.model}")
    private String model = "text-embedding-v2";

    @Override
    public float[] embed(String text) {
        // 调用阿里 embedding API
        // 返回 float[1024]
    }

    @Override
    public List<float[]> batchEmbed(List<String> texts) {
        // 批量接口
    }
}
```

- [ ] **Step 2: 编写单元测试（Mock 外部调用）**

```java
class EmbeddingServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private AliEmbeddingService service;

    @Test
    void shouldReturnEmbeddingVector_whenApiSuccess() {
        // Mock restTemplate 返回正确结果
        when(restTemplate.postForObject(anyString(), any(), any()))
            .thenReturn(createMockResponse());

        float[] embedding = service.embed("测试文本");

        assertThat(embedding).hasSize(1024);
    }
}
```

- [ ] **Step 3: 运行测试确认通过**

运行：`mvn test -Dtest=EmbeddingServiceTest -pl hify-module-rag`
预期：PASS

- [ ] **Step 4: 提交**

```bash
git add hify-module-rag/src/main/java/com/hify/rag/core/EmbeddingService.java \
      hify-module-rag/src/main/java/com/hify/rag/core/impl/AliEmbeddingService.java \
      hify-module-rag/src/test/java/com/hify/rag/core/EmbeddingServiceTest.java
git commit -m "feat(rag): implement AliEmbeddingService"
```

---

## Task 14: 实现 DocumentService（文档处理流程）

**类型：** 单元测试 TDD

**文件：**
- 创建：`hify-module-rag/src/main/java/com/hify/rag/service/DocumentService.java`
- 创建：`hify-module-rag/src/main/java/com/hify/rag/service/impl/DocumentServiceImpl.java`
- 创建：`hify-module-rag/src/test/java/com/hify/rag/service/DocumentServiceTest.java`

**依赖：** Task 7, Task 11, Task 12, Task 13

**描述：** 实现文档上传/解析/分块/向量化完整流程

- [ ] **Step 1: 编写单元测试**

```java
class DocumentServiceTest {

    @Mock
    private DocumentMapper documentMapper;

    @Mock
    private DocumentChunkMapper chunkMapper;

    @Mock
    private DocumentParserFactory parserFactory;

    @Mock
    private RecursiveChunker chunker;

    @Mock
    private EmbeddingService embeddingService;

    @InjectMocks
    private DocumentServiceImpl service;

    @Test
    void shouldCreateDocument_whenUploadFile() {
        // Given
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("test.txt");
        when(file.getSize()).thenReturn(1024L);

        // When
        Long docId = service.uploadDocument(1L, file);

        // Then
        verify(documentMapper).insert(any(Document.class));
    }
}
```

- [ ] **Step 2: 编写最小实现**

```java
@Service
public class DocumentServiceImpl implements DocumentService {

    @Autowired
    private DocumentMapper documentMapper;

    @Autowired
    private DocumentChunkMapper chunkMapper;

    @Autowired
    private DocumentParserFactory parserFactory;

    @Autowired
    private RecursiveChunker chunker;

    @Autowired
    private EmbeddingService embeddingService;

    @Async
    public void processDocumentAsync(Long documentId) {
        Document doc = documentMapper.selectById(documentId);
        doc.setStatus("processing");
        documentMapper.updateById(doc);

        try {
            // 1. 解析
            DocumentParser parser = parserFactory.getParser(doc.getFileType());
            String content = parser.parse(new FileInputStream(doc.getFilePath()));

            // 2. 分块
            List<String> chunks = chunker.chunk(content);

            // 3. 向量化 + 存储
            for (int i = 0; i < chunks.size(); i++) {
                float[] embedding = embeddingService.embed(chunks.get(i));
                saveChunk(doc, chunks.get(i), embedding, i);
            }

            doc.setStatus("completed");
            doc.setTotalChunks(chunks.size());
        } catch (Exception e) {
            doc.setStatus("failed");
            doc.setErrorMessage(e.getMessage());
        }
        documentMapper.updateById(doc);
    }
}
```

- [ ] **Step 3: 运行测试确认通过**

运行：`mvn test -Dtest=DocumentServiceTest -pl hify-module-rag`
预期：PASS

- [ ] **Step 4: 提交**

```bash
git add hify-module-rag/src/main/java/com/hify/rag/service/DocumentService.java \
      hify-module-rag/src/main/java/com/hify/rag/service/impl/DocumentServiceImpl.java \
      hify-module-rag/src/test/java/com/hify/rag/service/DocumentServiceTest.java
git commit -m "feat(rag): implement DocumentService with async processing"
```

---

## Task 15: 实现 RagSearchService（向量检索）

**类型：** 单元测试 TDD

**文件：**
- 创建：`hify-module-rag/src/main/java/com/hify/rag/service/RagSearchService.java`
- 创建：`hify-module-rag/src/main/java/com/hify/rag/service/impl/RagSearchServiceImpl.java`
- 创建：`hify-module-rag/src/main/java/com/hify/rag/vo/RagSearchResult.java`
- 创建：`hify-module-rag/src/test/java/com/hify/rag/service/RagSearchServiceTest.java`

**依赖：** Task 13

**描述：** 实现向量检索 + 阈值过滤

- [ ] **Step 1: 编写 VO**

```java
@Data
public class RagSearchResult {
    private Long chunkId;
    private String content;
    private Float similarity;
    private String metaJson;
}
```

- [ ] **Step 2: 编写 Service**

```java
public interface RagSearchService {
    List<RagSearchResult> search(Long kbId, String query, int topK, float threshold);
}

@Service
public class RagSearchServiceImpl implements RagSearchService {

    @Autowired
    private DocumentChunkMapper chunkMapper;

    @Autowired
    private EmbeddingService embeddingService;

    @Override
    public List<RagSearchResult> search(Long kbId, String query, int topK, float threshold) {
        // 1. query 向量化
        float[] queryEmbedding = embeddingService.embed(query);

        // 2. 向量检索（使用 pgvector 的 <=> 运算符）
        List<DocumentChunk> chunks = chunkMapper.vectorSearch(kbId, queryEmbedding, topK, threshold);

        // 3. 转换为结果
        return chunks.stream().map(chunk -> {
            RagSearchResult result = new RagSearchResult();
            result.setChunkId(chunk.getId());
            result.setContent(chunk.getContent());
            result.setSimilarity(calculateSimilarity(queryEmbedding, parseEmbedding(chunk.getEmbedding())));
            result.setMetaJson(chunk.getMetaJson());
            return result;
        }).collect(Collectors.toList());
    }
}
```

- [ ] **Step 3: 更新 DocumentChunkMapper（向量检索 SQL）**

```xml
<select id="vectorSearch" resultType="DocumentChunk">
    SELECT *, 1 - (embedding <=> #{queryEmbedding}::vector) AS similarity
    FROM document_chunk
    WHERE kb_id = #{kbId}
      AND enabled = true
      AND is_deleted = false
      AND 1 - (embedding <=> #{queryEmbedding}::vector) >= #{threshold}
    ORDER BY embedding <=> #{queryEmbedding}::vector
    LIMIT #{topK}
</select>
```

- [ ] **Step 4: 编写单元测试**

```java
class RagSearchServiceTest {
    // 测试向量检索逻辑
}
```

- [ ] **Step 5: 提交**

```bash
git add hify-module-rag/src/main/java/com/hify/rag/service/RagSearchService.java \
      hify-module-rag/src/main/java/com/hify/rag/service/impl/RagSearchServiceImpl.java \
      hify-module-rag/src/main/java/com/hify/rag/vo/RagSearchResult.java \
      hify-module-rag/src/main/resources/mapper/DocumentChunkMapper.xml
git commit -m "feat(rag): implement RagSearchService with vector search"
```

---

## Task 16: 实现 RagContextBuilder

**类型：** 单元测试 TDD

**文件：**
- 创建：`hify-module-rag/src/main/java/com/hify/rag/service/RagContextBuilder.java`

**依赖：** Task 15

**描述：** 将检索到的 chunks 拼接为 LLM 可用的上下文字符串

- [ ] **Step 1: 编写实现**

```java
@Service
public class RagContextBuilder {

    public String buildContext(List<RagSearchResult> results) {
        if (results.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("【参考知识】\n\n");

        for (int i = 0; i < results.size(); i++) {
            sb.append(String.format("[%d] %s\n\n", i + 1, results.get(i).getContent()));
        }

        sb.append("【问题】请根据以上参考知识回答用户问题。如果参考知识中没有相关信息，请如实说明。");
        return sb.toString();
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add hify-module-rag/src/main/java/com/hify/rag/service/RagContextBuilder.java
git commit -m "feat(rag): implement RagContextBuilder"
```

---

## Task 17: 实现 AgentKnowledgeBaseService

**类型：** 单元测试 TDD

**文件：**
- 创建：`hify-module-rag/src/main/java/com/hify/rag/service/AgentKnowledgeBaseService.java`
- 创建：`hify-module-rag/src/main/java/com/hify/rag/service/impl/AgentKnowledgeBaseServiceImpl.java`
- 创建：`hify-module-rag/src/test/java/com/hify/rag/service/AgentKnowledgeBaseServiceTest.java`

**依赖：** Task 8

**描述：** 实现 Agent 绑定知识库的管理服务

- [ ] **Step 1: 编写最小实现**

```java
@Service
public class AgentKnowledgeBaseServiceImpl implements AgentKnowledgeBaseApi {

    @Autowired
    private AgentKnowledgeBaseMapper mapper;

    @Override
    public void bind(AgentKbBindingDTO dto) {
        AgentKnowledgeBase binding = new AgentKnowledgeBase();
        binding.setAgentId(dto.getAgentId());
        binding.setKbId(dto.getKbId());
        binding.setTopK(dto.getTopK() != null ? dto.getTopK() : 5);
        binding.setSimilarityThreshold(dto.getSimilarityThreshold() != null
            ? dto.getSimilarityThreshold() : new BigDecimal("0.7"));
        binding.setEnabled(true);
        mapper.insert(binding);
    }

    @Override
    public List<AgentKnowledgeBaseDTO> getByAgentId(Long agentId) {
        return mapper.selectByAgentId(agentId);
    }

    // ... 其他方法
}
```

- [ ] **Step 2: 提交**

```bash
git add hify-module-rag/src/main/java/com/hify/rag/service/AgentKnowledgeBaseService.java \
      hify-module-rag/src/main/java/com/hify/rag/service/impl/AgentKnowledgeBaseServiceImpl.java
git commit -m "feat(rag): implement AgentKnowledgeBaseService"
```

---

## Task 18: 修改 ChatService 集成 RAG 上下文

**类型：** 单元测试 TDD

**文件：**
- 修改：`hify-module-chat/src/main/java/com/hify/chat/service/ChatService.java`
- 创建：`hify-module-chat/src/test/java/com/hify/chat/service/ChatServiceRagIT.java`

**依赖：** Task 16, Task 17

**描述：** 在调用 LLM 前，查询 Agent 绑定的知识库，检索相关 chunks，注入为 system prompt 前缀

- [ ] **Step 1: 修改 ChatService**

```java
@Service
public class ChatService {

    @Autowired
    private RagSearchService ragSearchService;

    @Autowired
    private RagContextBuilder ragContextBuilder;

    @Autowired
    private AgentKnowledgeBaseApi agentKnowledgeBaseApi;

    public ChatResponse chat(Long agentId, String message) {
        // 1. 获取 Agent 绑定的知识库
        List<AgentKnowledgeBaseDTO> bindings = agentKnowledgeBaseApi.getByAgentId(agentId);

        String ragContext = "";
        if (!bindings.isEmpty()) {
            // 2. 对每个知识库检索
            List<RagSearchResult> allResults = new ArrayList<>();
            for (AgentKnowledgeBaseDTO binding : bindings) {
                List<RagSearchResult> results = ragSearchService.search(
                    binding.getKbId(),
                    message,
                    binding.getTopK(),
                    binding.getSimilarityThreshold().floatValue()
                );
                allResults.addAll(results);
            }

            // 3. 构建 RAG 上下文
            if (!allResults.isEmpty()) {
                ragContext = ragContextBuilder.buildContext(allResults);
            }
        }

        // 4. 拼接 system prompt
        String systemPrompt = agent.getSystemPrompt();
        if (!ragContext.isEmpty()) {
            systemPrompt = ragContext + "\n\n" + systemPrompt;
        }

        // 5. 调用 LLM
        return llmClient.chat(systemPrompt, message);
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add hify-module-chat/src/main/java/com/hify/chat/service/ChatService.java
git commit -m "feat(chat): integrate RAG context into chat flow"
```

---

## Task 19: 创建 RAG Controller

**类型：** 结构变更

**文件：**
- 创建：`hify-module-rag/src/main/java/com/hify/rag/controller/KnowledgeBaseController.java`
- 创建：`hify-module-rag/src/main/java/com/hify/rag/controller/DocumentController.java`

**依赖：** Task 6, Task 7

**描述：** 创建 REST Controller，暴露 RAG API

- [ ] **Step 1: 编写 Controller**

```java
@RestController
@RequestMapping("/api/rag/knowledge-bases")
public class KnowledgeBaseController {

    @Autowired
    private KnowledgeBaseApi knowledgeBaseApi;

    @PostMapping
    public Long create(@RequestBody @Valid KnowledgeBaseCreateDTO dto) {
        return knowledgeBaseApi.create(dto);
    }

    @GetMapping
    public PageResult<KnowledgeBaseDTO> list(KnowledgeBaseQueryDTO query) {
        return knowledgeBaseApi.list(query);
    }

    @GetMapping("/{id}")
    public KnowledgeBaseDTO getById(@PathVariable Long id) {
        return knowledgeBaseApi.getById(id);
    }

    @PutMapping("/{id}")
    public void update(@PathVariable Long id, @RequestBody @Valid KnowledgeBaseUpdateDTO dto) {
        knowledgeBaseApi.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        knowledgeBaseApi.delete(id);
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add hify-module-rag/src/main/java/com/hify/rag/controller/KnowledgeBaseController.java \
      hify-module-rag/src/main/java/com/hify/rag/controller/DocumentController.java
git commit -m "feat(rag): add RAG REST controllers"
```

---

## Task 20-22: 前端页面

**类型：** 配置变更

**文件：**
- 创建：`hify-web/src/views/rag/KnowledgeBaseList.vue`
- 创建：`hify-web/src/views/rag/DocumentManage.vue`
- 创建：`hify-web/src/views/rag/AgentRagConfig.vue`

**依赖：** Task 18, Task 19（后端 API 完成）

**描述：** 创建前端页面（知识库管理、文档管理、Agent 配置）

- [ ] **Step 1: 创建知识库管理页面**

实现知识库的列表、创建、编辑、删除功能

- [ ] **Step 2: 创建文档管理页面**

实现文档上传、列表、删除、状态查看

- [ ] **Step 3: 创建 Agent RAG 配置页面**

实现 Agent 绑定知识库、配置 top_k 和 similarity_threshold

- [ ] **Step 4: 提交**

```bash
git add hify-web/src/views/rag/
git commit -m "feat(web): add RAG management pages"
```

---

## Task 23-25: 集成测试

**类型：** 集成测试 TDD

**文件：**
- 创建：`hify-module-rag/src/test/java/com/hify/rag/RagFlowIT.java`

**依赖：** Task 14, Task 15, Task 18

**描述：** 端到端测试：文档上传 → 分块 → 向量化 → 检索 → Agent 对话

- [ ] **Step 1: 编写集成测试**

```java
@SpringBootTest
@ActiveProfiles("test")
class RagFlowIT {

    @Autowired
    private DocumentService documentService;

    @Autowired
    private RagSearchService ragSearchService;

    @Test
    void shouldRetrieveChunks_whenDocumentProcessed() {
        // 1. 上传文档
        Long docId = documentService.uploadDocument(1L, testFile());

        // 2. 等待处理完成（异步）
        await().atMost(30, TimeUnit.SECONDS)
            .until(() -> documentService.getById(docId).getStatus(), is("completed"));

        // 3. 检索
        List<RagSearchResult> results = ragSearchService.search(1L, "测试查询", 5, 0.7f);

        // 4. 验证
        assertThat(results).isNotEmpty();
    }
}
```

- [ ] **Step 2: 运行测试确认通过**

- [ ] **Step 3: 提交**

```bash
git add hify-module-rag/src/test/java/com/hify/rag/RagFlowIT.java
git commit -m "test(rag): add end-to-end RAG flow integration test"
```