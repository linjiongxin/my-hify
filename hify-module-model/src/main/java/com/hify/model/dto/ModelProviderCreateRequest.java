package com.hify.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.Map;

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
    @Pattern(regexp = "^[a-z0-9_]+$", message = "代码只能包含小写字母、数字和下划线")
    private String code;

    @NotBlank(message = "协议类型不能为空")
    private String protocolType;

    @NotBlank(message = "API 基础地址不能为空")
    private String apiBaseUrl;

    @NotBlank(message = "鉴权类型不能为空")
    private String authType;

    private String apiKey;

    private Map<String, Object> authConfig;

    private Boolean enabled = true;

    private Integer sortOrder = 0;
}
