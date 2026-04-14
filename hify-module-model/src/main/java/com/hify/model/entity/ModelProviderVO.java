package com.hify.model.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 模型提供商视图对象
 *
 * @author hify
 */
@Data
public class ModelProviderVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    private Long id;

    /**
     * 提供商名称
     */
    private String name;

    /**
     * 提供商代码
     */
    private String code;

    /**
     * API 基础地址
     */
    private String apiBaseUrl;

    /**
     * 是否需要 API Key
     */
    private Boolean apiKeyRequired;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 排序
     */
    private Integer sortOrder;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
