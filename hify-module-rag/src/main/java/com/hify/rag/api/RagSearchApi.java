package com.hify.rag.api;

import com.hify.rag.vo.RagSearchResult;

import java.util.List;

/**
 * RAG 检索 API 接口（供 chat 模块调用）
 */
public interface RagSearchApi {

    /**
     * 在指定知识库中检索相关内容
     *
     * @param kbId      知识库 ID
     * @param query     查询文本
     * @param topK      返回数量
     * @param threshold 相似度阈值
     * @return 检索结果列表
     */
    List<RagSearchResult> search(Long kbId, String query, int topK, float threshold);
}
