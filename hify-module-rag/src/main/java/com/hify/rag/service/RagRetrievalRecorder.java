package com.hify.rag.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hify.rag.entity.RagRetrievalLog;
import com.hify.rag.mapper.RagRetrievalLogMapper;
import com.hify.rag.vo.RagSearchResult;
import com.hify.common.web.filter.TraceIdFilter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * RAG 检索日志记录器
 * <p>
 * 在 RAG 检索前后调用，记录检索参数、结果和耗时。
 * traceId 自动从 MDC 读取。
 */
@Slf4j
@Service
public class RagRetrievalRecorder {

    @Autowired
    private RagRetrievalLogMapper ragRetrievalLogMapper;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 记录检索开始，返回 logId 用于后续更新
     *
     * @param query 检索查询文本
     * @param kbId  知识库 ID
     * @return 日志记录 ID
     */
    public Long recordStart(String query, Long kbId) {
        try {
            RagRetrievalLog logEntity = new RagRetrievalLog();
            logEntity.setKbId(kbId);
            logEntity.setQuery(query);
            logEntity.setTraceId(MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY));
            ragRetrievalLogMapper.insert(logEntity);
            return logEntity.getId();
        } catch (Exception e) {
            log.warn("Failed to record RAG retrieval start", e);
            return null;
        }
    }

    /**
     * 记录检索成功结果
     *
     * @param logId        日志记录 ID（recordStart 返回值）
     * @param resultCount  返回结果数量
     * @param topChunks    返回的 chunks 列表
     * @param durationMs   检索耗时（毫秒）
     */
    public void recordSuccess(Long logId, Integer resultCount, List<?> topChunks, Long durationMs) {
        if (logId == null) {
            return;
        }
        try {
            RagRetrievalLog logEntity = new RagRetrievalLog();
            logEntity.setId(logId);
            logEntity.setResultCount(resultCount);
            logEntity.setDurationMs(durationMs != null ? durationMs.intValue() : null);

            if (topChunks != null && !topChunks.isEmpty()) {
                List<Map<String, Object>> chunkSummaries = topChunks.stream()
                        .map(this::toChunkSummary)
                        .collect(Collectors.toList());
                logEntity.setTopChunks(objectMapper.writeValueAsString(chunkSummaries));
            }

            ragRetrievalLogMapper.updateById(logEntity);
        } catch (Exception e) {
            log.warn("Failed to record RAG retrieval success for logId={}", logId, e);
        }
    }

    private Map<String, Object> toChunkSummary(Object chunk) {
        if (chunk instanceof RagSearchResult result) {
            return Map.of(
                    "chunkId", result.getChunkId(),
                    "content", truncate(result.getContent(), 200),
                    "similarity", result.getSimilarity()
            );
        }
        if (chunk instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of("value", chunk.toString());
    }

    private String truncate(String text, int maxLen) {
        if (text == null) {
            return "";
        }
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }
}
