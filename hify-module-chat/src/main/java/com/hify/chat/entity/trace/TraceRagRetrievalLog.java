package com.hify.chat.entity.trace;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hify.common.web.entity.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * RAG 检索日志（chat 模块只读视图）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("rag_retrieval_log")
public class TraceRagRetrievalLog extends BaseEntity {

    private Long kbId;
    private String query;
    private Integer resultCount;
    private String topChunks;
    private Integer durationMs;
    private String traceId;
}
