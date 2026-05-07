package com.hify.rag.vo;

import lombok.Data;

/**
 * 文档 VO
 */
@Data
public class DocumentVO {

    private Long id;

    private Long kbId;

    private String fileName;

    private String fileType;

    private Long fileSize;

    private String filePath;

    private String status;

    private String parsedContent;

    private Integer totalChunks;

    private String errorMessage;

    private java.time.LocalDateTime createdAt;
}