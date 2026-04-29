package com.hify.rag.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文档分块检索结果 VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChunkSearchVO {

    private Long id;

    private Long kbId;

    private Long documentId;

    private String content;

    private Integer chunkIndex;

    /**
     * 元数据（JSON 字符串），需要时用 Jackson 反序列化为 Map
     */
    private String metaJson;

    private Float similarity;
}
