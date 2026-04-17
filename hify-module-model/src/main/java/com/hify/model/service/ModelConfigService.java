package com.hify.model.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hify.common.web.entity.PageParam;
import com.hify.common.web.entity.PageResult;
import com.hify.model.dto.ModelConfigCreateRequest;
import com.hify.model.dto.ModelConfigUpdateRequest;
import com.hify.model.entity.ModelConfig;
import com.hify.model.vo.ModelConfigVO;

/**
 * 模型 Service
 *
 * @author hify
 */
public interface ModelConfigService extends IService<ModelConfig> {

    /**
     * 分页查询模型
     */
    PageResult<ModelConfigVO> pageModels(PageParam pageParam);

    /**
     * 获取模型详情
     */
    ModelConfigVO getModelDetail(Long id);

    /**
     * 创建模型
     */
    Long createModel(ModelConfigCreateRequest request);

    /**
     * 更新模型
     */
    void updateModel(Long id, ModelConfigUpdateRequest request);

    /**
     * 删除模型
     */
    void deleteModel(Long id);
}
