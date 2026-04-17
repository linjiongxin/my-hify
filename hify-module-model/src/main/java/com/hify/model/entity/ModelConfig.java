package com.hify.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.hify.common.web.entity.base.BaseEntity;
import com.hify.common.web.handler.JsonbTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 模型定义
 *
 * @author hify
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("model_config")
public class ModelConfig extends BaseEntity {

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
     * 上下文窗口长度
     */
    private Integer contextWindow;

    /**
     * 能力矩阵
     */
    @TableField(typeHandler = JsonbTypeHandler.class)
    private Map<String, Object> capabilities;

    /**
     * 输入价格（每百万 token）
     */
    @TableField("input_price_per_1m")
    private BigDecimal inputPricePer1m;

    /**
     * 输出价格（每百万 token）
     */
    @TableField("output_price_per_1m")
    private BigDecimal outputPricePer1m;

    /**
     * 是否为该提供商下的默认模型
     */
    @TableField("default_model")
    private Boolean defaultModel;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 排序
     */
    private Integer sortOrder;
}
