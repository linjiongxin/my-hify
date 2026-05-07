package com.hify.rag.service.impl;

import com.hify.rag.core.EmbeddingService;
import com.hify.rag.core.EmbeddingServiceFactory;
import com.hify.rag.entity.KnowledgeBase;
import com.hify.rag.mapper.DocumentChunkMapper;
import com.hify.rag.mapper.KnowledgeBaseMapper;
import com.hify.rag.api.RagSearchApi;
import com.hify.rag.service.RagSearchService;
import com.hify.rag.vo.ChunkSearchVO;
import com.hify.rag.vo.RagSearchResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * RAG 搜索服务实现
 */
@Slf4j
@Service
public class RagSearchServiceImpl implements RagSearchService, RagSearchApi {

    @Autowired
    private DocumentChunkMapper documentChunkMapper;

    @Autowired
    private KnowledgeBaseMapper knowledgeBaseMapper;

    @Autowired
    private EmbeddingServiceFactory embeddingServiceFactory;

    @Override
    public List<RagSearchResult> search(Long kbId, String query, int topK, float threshold) {
        try {
            // 1. 获取知识库配置（包含 embedding 模型）
            KnowledgeBase kb = knowledgeBaseMapper.selectById(kbId);
            if (kb == null) {
                log.error("Knowledge base not found: {}", kbId);
                return List.of();
            }

            // 根据知识库的 embedding 模型获取对应的服务
            EmbeddingService embeddingService = embeddingServiceFactory.getService(kb.getEmbeddingModel());
            log.debug("Using embedding service for model: {}", kb.getEmbeddingModel());

            // 2. Query 向量化
            float[] queryEmbedding = embeddingService.embed(query);
            String embeddingStr = vectorToString(queryEmbedding);

            // 3. 向量检索（使用 pgvector 的 <=> 运算符）
            // 多取候选，确保长内容有机会进入结果池
            List<ChunkSearchVO> chunks = documentChunkMapper.searchSimilarInKb(
                    embeddingStr, kbId, topK * 20);

            // 4. 过滤 + 去重
            // 注：不过滤短内容，因为标题 chunk 虽短但语义匹配度高，
            // 且 nomic-embed-text 对中文长文本的区分度不足，长内容可能排名靠后
            List<RagSearchResult> results = chunks.stream()
                    .filter(chunk -> chunk.getSimilarity() >= threshold)
                    .filter(chunk -> chunk.getContent() != null && !chunk.getContent().trim().isEmpty())
                    .map(chunk -> {
                        RagSearchResult result = new RagSearchResult();
                        result.setChunkId(chunk.getId());
                        result.setContent(chunk.getContent().trim());
                        result.setSimilarity(chunk.getSimilarity());
                        result.setMetaJson(chunk.getMetaJson());
                        return result;
                    })
                    // 按内容去重（避免重复文档/重复 chunk 占满结果）
                    .collect(Collectors.collectingAndThen(
                            Collectors.toMap(RagSearchResult::getContent, r -> r,
                                    (a, b) -> a.getSimilarity() > b.getSimilarity() ? a : b),
                            map -> map.values().stream()
                                    .sorted((a, b) -> Float.compare(b.getSimilarity(), a.getSimilarity()))
                                    .limit(topK)
                                    .collect(Collectors.toList())
                    ));

            log.debug("RAG search for kbId={}, query='{}', found {} results",
                    kbId, query.substring(0, Math.min(50, query.length())), results.size());

            return results;

        } catch (Exception e) {
            log.error("RAG search failed for kbId={}, query='{}'",
                    kbId, query.substring(0, Math.min(50, query.length())), e);
            return List.of();
        }
    }

    private String vectorToString(float[] vector) {
        if (vector == null || vector.length == 0) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(vector[i]);
        }
        sb.append("]");
        return sb.toString();
    }
}