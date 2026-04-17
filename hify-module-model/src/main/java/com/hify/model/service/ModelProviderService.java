package com.hify.model.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hify.common.web.entity.PageParam;
import com.hify.common.web.entity.PageResult;
import com.hify.model.dto.ModelProviderCreateRequest;
import com.hify.model.dto.ModelProviderUpdateRequest;
import com.hify.model.entity.ModelProvider;
import com.hify.model.vo.ModelProviderVO;

/**
 * 模型提供商 Service
 *
 * @author hify
 */
public interface ModelProviderService extends IService<ModelProvider> {

    /**
     * 分页查询模型提供商
     */
    PageResult<ModelProviderVO> pageProviders(PageParam pageParam);

    /**
     * 获取模型提供商详情
     */
    ModelProviderVO getProviderDetail(Long id);

    /**
     * 创建模型提供商
     */
    Long createProvider(ModelProviderCreateRequest request);

    /**
     * 更新模型提供商
     */
    void updateProvider(Long id, ModelProviderUpdateRequest request);

    /**
     * 删除模型提供商
     */
    void deleteProvider(Long id);
}
