package com.hify.rag.service;

import com.hify.rag.api.AgentKnowledgeBaseApi;
import com.hify.rag.api.KnowledgeBaseApi;
import com.hify.rag.service.RagSearchService;
import com.hify.rag.service.RagContextBuilder;
import com.hify.rag.vo.AgentKnowledgeBaseVO;
import com.hify.rag.vo.RagSearchResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * RAG 上下文注入器
 * <p>
 * 在调用 LLM 前，查询 Agent 绑定的知识库，检索相关 chunks，注入为 system prompt 前缀
 */
@Slf4j
@Service
public class RagContextInjector {

    @Autowired
    private AgentKnowledgeBaseApi agentKnowledgeBaseApi;

    @Autowired
    private RagSearchService ragSearchService;

    @Autowired
    private RagContextBuilder ragContextBuilder;

    @Autowired
    private RagRetrievalRecorder ragRetrievalRecorder;

    /**
     * 为指定 Agent 获取 RAG 上下文字符串
     *
     * @param agentId Agent ID
     * @param userMessage 用户消息（用于检索）
     * @return RAG 上下文字符串，如果无绑定知识库则返回空字符串
     */
    public String getRagContext(Long agentId, String userMessage) {
        if (agentId == null || userMessage == null || userMessage.isBlank()) {
            return "";
        }

        try {
            // 1. 获取 Agent 绑定的知识库
            List<AgentKnowledgeBaseVO> bindings = agentKnowledgeBaseApi.getByAgentId(agentId);
            if (bindings == null || bindings.isEmpty()) {
                log.debug("Agent {} has no knowledge base bindings", agentId);
                return "";
            }

            // 2. 对每个知识库检索
            List<RagSearchResult> allResults = new ArrayList<>();
            for (AgentKnowledgeBaseVO binding : bindings) {
                Long logId = ragRetrievalRecorder.recordStart(userMessage, binding.getKbId());
                long startMs = System.currentTimeMillis();
                List<RagSearchResult> results = ragSearchService.search(
                        binding.getKbId(),
                        userMessage,
                        binding.getTopK(),
                        binding.getSimilarityThreshold().floatValue()
                );
                long durationMs = System.currentTimeMillis() - startMs;
                ragRetrievalRecorder.recordSuccess(logId, results.size(), results, durationMs);
                allResults.addAll(results);
            }

            // 3. 构建 RAG 上下文
            if (allResults.isEmpty()) {
                log.debug("No relevant chunks found for agent {} query: {}",
                        agentId, userMessage.substring(0, Math.min(30, userMessage.length())));
                return "";
            }

            String ragContext = ragContextBuilder.buildContext(allResults);
            log.info("RAG context built for agent {}, {} chunks", agentId, allResults.size());
            return ragContext;

        } catch (Exception e) {
            log.error("Failed to build RAG context for agent {}", agentId, e);
            return "";
        }
    }

    /**
     * 注入 RAG 上下文到 System Prompt
     */
    public String injectToSystemPrompt(Long agentId, String userMessage, String originalSystemPrompt) {
        String ragContext = getRagContext(agentId, userMessage);
        if (ragContext.isEmpty()) {
            return originalSystemPrompt;
        }
        return ragContext + "\n\n" + (originalSystemPrompt != null ? originalSystemPrompt : "");
    }
}