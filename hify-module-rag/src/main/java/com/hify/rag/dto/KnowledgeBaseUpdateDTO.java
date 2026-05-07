package com.hify.rag.dto;

import lombok.Data;

/**
 * 更新知识库 DTO
 */
@Data
public class KnowledgeBaseUpdateDTO {

    /**
     * 知识库名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;

    /**
     * Embedding 模型
     */
    private String embeddingModel;

    /**
     * 分块大小
     */
    private Integer chunkSize;

    /**
     * 分块重叠大小
     */
    private Integer chunkOverlap;

    /**
     * 是否启用
     */
    private Boolean enabled;
}