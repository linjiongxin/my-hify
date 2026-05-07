package com.hify.rag.vo;

import lombok.Data;

/**
 * RAG 检索结果
 */
@Data
public class RagSearchResult {

    /**
     * 分块 ID
     */
    private Long chunkId;

    /**
     * 分块内容
     */
    private String content;

    /**
     * 相似度
     */
    private Float similarity;

    /**
     * 元数据 JSON
     */
    private String metaJson;
}