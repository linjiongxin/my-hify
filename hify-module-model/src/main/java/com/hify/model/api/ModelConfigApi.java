package com.hify.model.api;

import com.hify.model.api.dto.ModelConfigDTO;

import java.util.List;

/**
 * 模型 API
 * <p>供其他模块调用的唯一入口</p>
 *
 * @author hify
 */
public interface ModelConfigApi {

    /**
     * 根据模型标识获取模型详情
     */
    ModelConfigDTO getModelByModelId(String modelId);

    /**
     * 获取指定提供商下所有启用的模型
     */
    List<ModelConfigDTO> listEnabledModelsByProviderId(Long providerId);
}
