package com.hify.rag.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.hify.common.web.entity.base.BaseEntity;
import com.hify.common.web.handler.JsonbStringTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * RAG 检索日志实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("rag_retrieval_log")
public class RagRetrievalLog extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 知识库 ID
     */
    private Long kbId;

    /**
     * 检索查询文本
     */
    private String query;

    /**
     * 返回结果数量
     */
    private Integer resultCount;

    /**
     * 返回的 top chunks（JSONB）
     */
    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String topChunks;

    /**
     * 检索耗时（毫秒）
     */
    private Integer durationMs;

    /**
     * 链路追踪 ID
     */
    private String traceId;
}
