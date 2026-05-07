package com.hify.rag.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.hify.common.web.entity.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文档实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("document")
public class Document extends BaseEntity {

    /**
     * 知识库 ID
     */
    private Long kbId;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件类型
     */
    private String fileType;

    /**
     * 文件大小
     */
    private Long fileSize;

    /**
     * 文件存储路径
     */
    private String filePath;

    /**
     * 状态：pending, processing, completed, failed
     */
    private String status;

    /**
     * 解析后的纯文本内容（可选，文件存储时可为空）
     */
    private String parsedContent;

    /**
     * 总分块数
     */
    private Integer totalChunks;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 逻辑删除字段（映射到 is_deleted 列）
     */
    @TableField("is_deleted")
    @com.baomidou.mybatisplus.annotation.TableLogic
    private Boolean deleted;
}