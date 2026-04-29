package com.hify.rag.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hify.common.web.entity.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文档分块实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("document_chunk")
public class DocumentChunk extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 知识库 ID
     */
    private Long kbId;

    /**
     * 文档 ID
     */
    private Long documentId;

    /**
     * 分块内容
     */
    private String content;

    /**
     * 向量（pgvector 类型，MyBatis-Plus 无法直接映射，用 String 存储）
     * 插入时通过 ::vector cast 转换
     */
    private String embedding;

    /**
     * 分块顺序
     */
    private Integer chunkIndex;

    /**
     * 扩展元数据（JSON 字符串），插入时通过 ::jsonb cast 转换
     */
    private String metaJson;

    /**
     * 是否启用
     */
    private Boolean enabled;
}
