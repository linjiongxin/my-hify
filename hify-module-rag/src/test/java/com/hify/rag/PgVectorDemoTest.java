package com.hify.rag;

import com.hify.rag.entity.DocumentChunk;
import com.hify.rag.mapper.DocumentChunkMapper;
import com.hify.rag.vo.ChunkSearchVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * pgvector RAG 演示测试
 *
 * <p>本测试演示完整的 RAG 流程：
 * <pre>
 * 1. 创建测试表（3 维向量，方便肉眼验证）
 * 2. 插入带向量的文档分块
 * 3. 执行余弦相似度检索
 * 4. 验证检索结果
 * </pre>
 *
 * <p>向量维度说明：
 * <ul>
 *   <li>本测试使用 3 维向量，便于手动验证计算结果</li>
 *   <li>生产环境使用 1536 维（OpenAI text-embedding-3-small）</li>
 * </ul>
 */
@SpringBootTest(classes = RagDemoApplication.class)
@ActiveProfiles("test")
class PgVectorDemoTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DocumentChunkMapper chunkMapper;

    private static final int VECTOR_DIM = 3;  // 测试用 3 维，生产用 1536

    /**
     * 演示数据：
     * - 苹果、香蕉是水果（向量接近）
     * - 汽车是交通工具（向量远离水果）
     */
    @Test
    void demo_pgvector_search_flow() {
        // ========== 阶段 1: 建表（3 维向量） ==========
        dropTableIfExists("demo_chunk_3d");
        jdbcTemplate.execute("""
            CREATE TABLE demo_chunk_3d (
                id BIGSERIAL PRIMARY KEY,
                content TEXT NOT NULL,
                embedding VECTOR(3),
                meta_json JSONB
            )
            """);
        jdbcTemplate.execute("""
            CREATE INDEX ON demo_chunk_3d
            USING hnsw (embedding vector_cosine_ops)
            """);

        // ========== 阶段 2: 插入测试数据 ==========
        // 向量说明：
        // - 苹果 [0.1, 0.8, 0.3] 和 香蕉 [0.2, 0.9, 0.1] 相似（水果，y 分量高）
        // - 汽车 [0.9, 0.1, 0.2] 远离水果（x 分量高，y 分量低）

        jdbcTemplate.execute("DELETE FROM demo_chunk_3d");

        jdbcTemplate.update("""
            INSERT INTO demo_chunk_3d (content, embedding, meta_json)
            VALUES (?, ?::vector, ?::jsonb)
            """, "苹果是一种水果，富含维生素C", "[0.1, 0.8, 0.3]",
            "{\"category\": \"食品\", \"title\": \"水果营养百科\"}");

        jdbcTemplate.update("""
            INSERT INTO demo_chunk_3d (content, embedding, meta_json)
            VALUES (?, ?::vector, ?::jsonb)
            """, "香蕉是一种热带水果", "[0.2, 0.9, 0.1]",
            "{\"category\": \"食品\", \"title\": \"水果营养百科\"}");

        jdbcTemplate.update("""
            INSERT INTO demo_chunk_3d (content, embedding, meta_json)
            VALUES (?, ?::vector, ?::jsonb)
            """, "汽车是一种交通工具，用于陆地行驶", "[0.9, 0.1, 0.2]",
            "{\"category\": \"交通\", \"title\": \"交通工具大全\"}");

        jdbcTemplate.update("""
            INSERT INTO demo_chunk_3d (content, embedding, meta_json)
            VALUES (?, ?::vector, ?::jsonb)
            """, "橘子是一种水果，味道酸甜", "[0.15, 0.85, 0.2]",
            "{\"category\": \"食品\", \"title\": \"水果营养百科\"}");

        // ========== 阶段 3: 检索 ==========
        // 查询向量：热带水果 [0.15, 0.95, 0.1]
        // 期望：香蕉 > 橘子 > 苹果 > 汽车

        String queryVector = "[0.15, 0.95, 0.1]";
        List<Map<String, Object>> results = jdbcTemplate.queryForList("""
            SELECT id, content,
                   (1 - (embedding <=> ?::vector)) AS similarity
            FROM demo_chunk_3d
            ORDER BY embedding <=> ?::vector
            LIMIT 4
            """, queryVector, queryVector);

        // ========== 阶段 4: 验证结果 ==========
        System.out.println("\n========== RAG 检索结果 ==========");
        for (Map<String, Object> row : results) {
            System.out.printf("ID: %s | 内容: %s | 相似度: %.4f%n",
                row.get("id"), row.get("content"), row.get("similarity"));
        }

        // 验证第一个结果是水果类
        assertTrue(((String) results.get(0).get("content")).contains("水果"),
            "第一个结果应该是水果相关");

        // 验证相似度排序：香蕉 > 橘子 > 苹果 > 汽车
        double sim0 = (double) results.get(0).get("similarity");
        double sim1 = (double) results.get(1).get("similarity");
        double sim2 = (double) results.get(2).get("similarity");
        double sim3 = (double) results.get(3).get("similarity");

        assertTrue(sim0 > sim1, "香蕉相似度 > 橘子");
        assertTrue(sim1 > sim2, "橘子相似度 > 苹果");
        assertTrue(sim2 > sim3, "苹果相似度 > 汽车");
        assertTrue(sim3 < 0.5, "汽车应该远离水果向量");

        System.out.println("\n========== 验证通过！==========");
        System.out.println("水果类文档排在最前面，证明向量检索正常工作");
    }

    /**
     * 使用 Mapper 查询（实际项目用法）
     */
    @Test
    void demo_mapper_search() {
        // 确保表和索引存在
        ensureProductionTable();

        // 插入一条测试数据（生产环境由 Embedding 模型生成向量）
        jdbcTemplate.update("""
            INSERT INTO document_chunk (kb_id, document_id, content, embedding, chunk_index, meta_json, enabled)
            VALUES (?, ?, ?, ?::vector, ?, ?::jsonb, ?)
            """,
            1L, 1L,
            "这是产品退货政策：自收到商品之日起7天内可申请退货",
            "[0.1, 0.2, 0.3]",  // 假向量，仅演示
            0,
            "{\"title\": \"退换货政策\", \"source\": \"客服手册.pdf\"}",
            true
        );

        // 执行检索（生产环境：问题 → Embedding → 检索）
        // 这里用假向量演示，实际用 ChunkSearchVO 的 embedding 字段传入问题向量
        List<ChunkSearchVO> chunks = chunkMapper.searchSimilar("[0.1, 0.2, 0.3]", 5);

        System.out.println("\n========== Mapper 检索结果 ==========");
        for (ChunkSearchVO chunk : chunks) {
            System.out.printf("ID: %d | 内容: %s | 相似度: %.4f%n",
                chunk.getId(), chunk.getContent(), chunk.getSimilarity());
        }

        assertFalse(chunks.isEmpty(), "应该有检索结果");
    }

    /**
     * 纯 MyBatis-Plus 风格演示：
     * - 插入用 BaseMapper.insert() / 自定义 insertVector()
     * - 查询用 chunkMapper.searchSimilar()
     * - 不写一行原生 SQL（建表除外）
     */
    @Test
    void demo_mybatis_plus_style() {
        // ========== 阶段 1: 建表（只建一次，后续用 MP 操作）==========
        ensureProductionTable();

        // ========== 阶段 2: 用 MyBatis-Plus 插入向量数据 ==========
        // 模拟 Embedding 模型返回的向量（实际来自 API）
        String vectorApple = "[0.1, 0.8, 0.3]";
        String vectorBanana = "[0.2, 0.9, 0.1]";
        String vectorCar = "[0.9, 0.1, 0.2]";

        DocumentChunk chunk1 = new DocumentChunk();
        chunk1.setKbId(1L);
        chunk1.setDocumentId(1L);
        chunk1.setContent("苹果是一种水果，富含维生素C");
        chunk1.setEmbedding(vectorApple);
        chunk1.setChunkIndex(0);
        chunk1.setMetaJson("{\"category\": \"食品\", \"title\": \"水果营养\"}");
        chunk1.setEnabled(true);

        DocumentChunk chunk2 = new DocumentChunk();
        chunk2.setKbId(1L);
        chunk2.setDocumentId(1L);
        chunk2.setContent("香蕉是一种热带水果");
        chunk2.setEmbedding(vectorBanana);
        chunk2.setChunkIndex(1);
        chunk2.setMetaJson("{\"category\": \"食品\", \"title\": \"水果营养\"}");
        chunk2.setEnabled(true);

        DocumentChunk chunk3 = new DocumentChunk();
        chunk3.setKbId(1L);
        chunk3.setDocumentId(2L);
        chunk3.setContent("汽车是一种交通工具");
        chunk3.setEmbedding(vectorCar);
        chunk3.setChunkIndex(0);
        chunk3.setMetaJson("{\"category\": \"交通\", \"title\": \"交通工具\"}");
        chunk3.setEnabled(true);

        // 使用自定义 insertVector（XML mapper 实现，处理 ::vector cast）
        chunkMapper.insertVector(chunk1);
        chunkMapper.insertVector(chunk2);
        chunkMapper.insertVector(chunk3);

        // ========== 阶段 3: 用 MyBatis-Plus 查询 ==========
        // 模拟用户提问"什么是热带水果"的向量
        String queryVector = "[0.15, 0.95, 0.1]";  // 接近水果向量
        List<ChunkSearchVO> results = chunkMapper.searchSimilar(queryVector, 5);

        // ========== 阶段 4: 验证 ==========
        System.out.println("\n========== MyBatis-Plus 风格检索 ==========");
        for (ChunkSearchVO c : results) {
            System.out.printf("ID: %d | 内容: %s | 相似度: %.4f%n",
                c.getId(), c.getContent(), c.getSimilarity());
        }

        assertFalse(results.isEmpty(), "应该有结果");
        assertTrue(results.get(0).getContent().contains("水果"), "第一个应该是水果");
        assertTrue(results.get(0).getSimilarity() > results.get(1).getSimilarity(),
            "水果应该比汽车相似度更高");

        System.out.println("\n========== MP 风格验证通过！==========");
        System.out.println("insertVector + searchSimilar 全程不写原生 SQL");
    }

    private void ensureProductionTable() {
        // 确保 document_chunk 表存在（由 init SQL 创建）
        // 使用 3 维向量便于测试（生产用 1536 维）
        dropTableIfExists("document_chunk");
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS document_chunk (
                id BIGSERIAL PRIMARY KEY,
                kb_id BIGINT NOT NULL,
                document_id BIGINT NOT NULL,
                content TEXT NOT NULL,
                embedding VECTOR(3),
                chunk_index INT DEFAULT 0,
                meta_json JSONB,
                enabled BOOLEAN DEFAULT TRUE,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                deleted BOOLEAN DEFAULT FALSE
            )
            """);
        // 测试用小维度索引
        jdbcTemplate.execute("""
            CREATE INDEX IF NOT EXISTS idx_demo_embedding
            ON document_chunk USING hnsw (embedding vector_cosine_ops)
            """);
    }

    private void dropTableIfExists(String tableName) {
        jdbcTemplate.execute("DROP TABLE IF EXISTS " + tableName);
    }
}
