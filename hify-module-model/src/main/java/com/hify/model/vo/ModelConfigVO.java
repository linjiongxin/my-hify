package com.hify.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 模型视图对象
 *
 * @author hify
 */
@Data
public class ModelConfigVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    private Long id;

    /**
     * 提供商 ID
     */
    private Long providerId;

    /**
     * 模型名称
     */
    private String name;

    /**
     * 模型标识
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
    private Map<String, Object> capabilities;

    /**
     * 输入价格（每百万 token）
     */
    private BigDecimal inputPricePer1m;

    /**
     * 输出价格（每百万 token）
     */
    private BigDecimal outputPricePer1m;

    /**
     * 是否为默认模型
     */
    private Boolean defaultModel;

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
