package com.hify.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 更新模型提供商请求
 *
 * @author hify
 */
@Data
public class ModelProviderUpdateRequest {

    @NotBlank(message = "名称不能为空")
    private String name;

    @NotBlank(message = "API 基础地址不能为空")
    private String apiBaseUrl;

    private Boolean apiKeyRequired;

    private Boolean enabled;

    private Integer sortOrder;
}
