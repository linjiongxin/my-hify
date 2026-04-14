package com.hify.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建模型提供商请求
 *
 * @author hify
 */
@Data
public class ModelProviderCreateRequest {

    @NotBlank(message = "名称不能为空")
    private String name;

    @NotBlank(message = "代码不能为空")
    private String code;

    @NotBlank(message = "API 基础地址不能为空")
    private String apiBaseUrl;

    @NotNull(message = "是否需要 API Key 不能为空")
    private Boolean apiKeyRequired;

    private Boolean enabled = true;

    private Integer sortOrder = 0;
}
