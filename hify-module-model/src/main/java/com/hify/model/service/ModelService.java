package com.hify.model.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hify.common.web.entity.PageParam;
import com.hify.common.web.entity.PageResult;
import com.hify.model.dto.ModelCreateRequest;
import com.hify.model.dto.ModelUpdateRequest;
import com.hify.model.entity.Model;
import com.hify.model.entity.ModelVO;

/**
 * 模型 Service
 *
 * @author hify
 */
public interface ModelService extends IService<Model> {

    /**
     * 分页查询模型
     */
    PageResult<ModelVO> pageModels(PageParam pageParam);

    /**
     * 获取模型详情
     */
    ModelVO getModelDetail(Long id);

    /**
     * 创建模型
     */
    Long createModel(ModelCreateRequest request);

    /**
     * 更新模型
     */
    void updateModel(Long id, ModelUpdateRequest request);

    /**
     * 删除模型
     */
    void deleteModel(Long id);
}
