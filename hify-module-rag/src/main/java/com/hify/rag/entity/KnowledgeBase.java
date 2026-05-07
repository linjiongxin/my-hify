package com.hify.rag.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hify.common.web.entity.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 知识库实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("knowledge_base")
public class KnowledgeBase extends BaseEntity {

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

    /**
     * 逻辑删除字段（映射到 is_deleted 列）
     */
    @com.baomidou.mybatisplus.annotation.TableField("is_deleted")
    @com.baomidou.mybatisplus.annotation.TableLogic
    private Boolean deleted;
}