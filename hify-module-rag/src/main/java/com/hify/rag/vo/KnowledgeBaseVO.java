package com.hify.rag.vo;

import lombok.Data;

/**
 * 知识库 VO
 */
@Data
public class KnowledgeBaseVO {

    private Long id;

    private String name;

    private String description;

    private String embeddingModel;

    private Integer chunkSize;

    private Integer chunkOverlap;

    private Boolean enabled;

    private java.time.LocalDateTime createdAt;
}