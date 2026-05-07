package com.hify.rag.service;

import com.hify.rag.vo.RagSearchResult;

import java.util.List;

/**
 * RAG 搜索服务接口
 */
public interface RagSearchService {

    /**
     * 在指定知识库中检索相关内容
     *
     * @param kbId       知识库 ID
     * @param query      查询文本
     * @param topK       返回数量
     * @param threshold  相似度阈值
     * @return 检索结果列表
     */
    List<RagSearchResult> search(Long kbId, String query, int topK, float threshold);
}