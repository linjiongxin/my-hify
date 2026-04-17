package com.hify.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 更新模型请求
 *
 * @author hify
 */
@Data
public class ModelConfigUpdateRequest {

    @NotBlank(message = "名称不能为空")
    private String name;

    private Integer maxTokens;

    private Integer contextWindow;

    private Map<String, Object> capabilities;

    private BigDecimal inputPricePer1m;

    private BigDecimal outputPricePer1m;

    private Boolean defaultModel;

    private Boolean enabled;

    private Integer sortOrder;
}
