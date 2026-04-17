package com.hify.model.api;

import com.hify.model.api.dto.ModelProviderDTO;

import java.util.List;

/**
 * 模型提供商 API
 * <p>供其他模块调用的唯一入口</p>
 *
 * @author hify
 */
public interface ModelProviderApi {

    /**
     * 获取所有启用的模型提供商
     */
    List<ModelProviderDTO> listEnabledProviders();

    /**
     * 根据 ID 获取提供商详情
     */
    ModelProviderDTO getProviderById(Long id);
}
