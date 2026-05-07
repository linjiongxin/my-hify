package com.hify.rag.core;

/**
 * Embedding 服务接口
 */
public interface EmbeddingService {

    /**
     * 将文本转为向量
     */
    float[] embed(String text);

    /**
     * 批量将文本转为向量
     */
    java.util.List<float[]> batchEmbed(java.util.List<String> texts);
}