package com.hify.model.entity;

import com.hify.common.web.entity.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 模型提供商
 *
 * @author hify
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("model_provider")
public class ModelProvider extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 提供商名称
     */
    private String name;

    /**
     * 提供商代码（如 openai / deepseek / qwen）
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
}
