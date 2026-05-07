package com.hify.rag.service;

import com.hify.rag.vo.RagSearchResult;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * RAG 上下文构建器
 * <p>
 * 将检索到的 chunks 拼接为 LLM 可用的上下文
 */
@Service
public class RagContextBuilder {

    /**
     * 构建 RAG 上下文
     */
    public String buildContext(List<RagSearchResult> results) {
        if (results == null || results.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("【参考知识】\n\n");

        for (int i = 0; i < results.size(); i++) {
            sb.append(String.format("[%d] %s\n\n", i + 1, results.get(i).getContent()));
        }

        sb.append("请根据以上参考知识回答用户问题。如果参考知识中没有相关信息，请如实说明。");
        return sb.toString();
    }

    /**
     * 构建带来源标注的上下文
     */
    public String buildContextWithSource(List<RagSearchResult> results) {
        if (results == null || results.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("【参考知识】\n\n");

        for (int i = 0; i < results.size(); i++) {
            RagSearchResult r = results.get(i);
            sb.append(String.format("[%d] (相似度: %.2f) %s\n\n",
                    i + 1, r.getSimilarity(), r.getContent()));
        }

        sb.append("请根据以上参考知识回答用户问题。如果参考知识中没有相关信息，请如实说明。");
        return sb.toString();
    }
}