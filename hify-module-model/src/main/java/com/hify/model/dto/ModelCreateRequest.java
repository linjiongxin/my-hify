package com.hify.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 创建模型请求
 *
 * @author hify
 */
@Data
public class ModelCreateRequest {

    @NotNull(message = "提供商 ID 不能为空")
    private Long providerId;

    @NotBlank(message = "名称不能为空")
    private String name;

    @NotBlank(message = "模型标识不能为空")
    private String modelId;

    private Integer maxTokens;

    private Integer contextWindow;

    private Map<String, Object> capabilities;

    private BigDecimal inputPricePer1m;

    private BigDecimal outputPricePer1m;

    private Boolean isDefault = false;

    private Boolean enabled = true;

    private Integer sortOrder = 0;
}
