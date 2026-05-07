package com.hify.rag.service.impl;

import com.hify.rag.core.EmbeddingService;
import com.hify.rag.mapper.DocumentChunkMapper;
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
public class RagSearchServiceImpl implements RagSearchService {

    @Autowired
    private DocumentChunkMapper documentChunkMapper;

    @Autowired
    private EmbeddingService embeddingService;

    @Override
    public List<RagSearchResult> search(Long kbId, String query, int topK, float threshold) {
        try {
            // 1. Query 向量化
            float[] queryEmbedding = embeddingService.embed(query);
            String embeddingStr = vectorToString(queryEmbedding);

            // 2. 向量检索（使用 pgvector 的 <=> 运算符）
            // 注：阈值过滤在应用层处理，因为 pgvector 的距离运算符需要特殊处理
            List<ChunkSearchVO> chunks = documentChunkMapper.searchSimilarInKb(
                    embeddingStr, kbId, topK * 2); // 多取一些，后面过滤

            // 3. 过滤相似度阈值
            List<RagSearchResult> results = chunks.stream()
                    .filter(chunk -> chunk.getSimilarity() >= threshold)
                    .limit(topK)
                    .map(chunk -> {
                        RagSearchResult result = new RagSearchResult();
                        result.setChunkId(chunk.getId());
                        result.setContent(chunk.getContent());
                        result.setSimilarity(chunk.getSimilarity());
                        result.setMetaJson(chunk.getMetaJson());
                        return result;
                    })
                    .collect(Collectors.toList());

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