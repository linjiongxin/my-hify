package com.hify.model.entity;

import com.hify.common.web.entity.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 模型
 *
 * @author hify
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("model")
public class Model extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 提供商 ID
     */
    private Long providerId;

    /**
     * 模型名称（展示用）
     */
    private String name;

    /**
     * 模型标识（调用 API 时传入，如 gpt-4o）
     */
    private String modelId;

    /**
     * 最大 Token 数
     */
    private Integer maxTokens;

    /**
     * 是否启用
     */
    private Boolean enabled;
}
