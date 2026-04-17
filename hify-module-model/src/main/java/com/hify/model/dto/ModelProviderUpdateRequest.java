package com.hify.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

/**
 * 更新模型提供商请求
 *
 * @author hify
 */
@Data
public class ModelProviderUpdateRequest {

    @NotBlank(message = "名称不能为空")
    private String name;

    @NotBlank(message = "协议类型不能为空")
    private String protocolType;

    @NotBlank(message = "API 基础地址不能为空")
    private String apiBaseUrl;

    @NotBlank(message = "鉴权类型不能为空")
    private String authType;

    private String apiKey;

    private Map<String, Object> authConfig;

    private Boolean enabled;

    private Integer sortOrder;
}
