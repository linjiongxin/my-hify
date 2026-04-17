package com.hify.model.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hify.common.web.entity.PageParam;
import com.hify.common.web.entity.PageResult;
import com.hify.common.core.enums.ResultCode;
import com.hify.common.core.exception.BizException;
import com.hify.model.dto.ModelConfigCreateRequest;
import com.hify.model.dto.ModelConfigUpdateRequest;
import com.hify.model.api.ModelConfigApi;
import com.hify.model.api.dto.ModelConfigDTO;
import com.hify.model.entity.ModelConfig;
import com.hify.model.vo.ModelConfigVO;
import com.hify.model.mapper.ModelConfigMapper;
import com.hify.model.service.ModelConfigService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 模型 Service 实现
 *
 * @author hify
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class ModelConfigServiceImpl extends ServiceImpl<ModelConfigMapper, ModelConfig> implements ModelConfigService, ModelConfigApi {

    @Override
    @Transactional(readOnly = true)
    public PageResult<ModelConfigVO> pageModels(PageParam pageParam) {
        LambdaQueryWrapper<ModelConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(ModelConfig::getSortOrder).orderByAsc(ModelConfig::getId);
        return PageResult.of(baseMapper.selectPage(pageParam.toPage(ModelConfig.class), wrapper)
                .convert(this::toVO));
    }

    @Override
    @Transactional(readOnly = true)
    public ModelConfigVO getModelDetail(Long id) {
        ModelConfig model = getById(id);
        if (model == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "数据不存在");
        }
        return toVO(model);
    }

    @Override
    public Long createModel(ModelConfigCreateRequest request) {
        // 如果设置为默认模型，取消该 provider 下其他默认模型
        if (Boolean.TRUE.equals(request.getDefaultModel())) {
            clearDefaultModel(request.getProviderId());
        }
        ModelConfig model = new ModelConfig();
        BeanUtils.copyProperties(request, model);
        save(model);
        return model.getId();
    }

    @Override
    public void updateModel(Long id, ModelConfigUpdateRequest request) {
        ModelConfig model = getById(id);
        if (model == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "数据不存在");
        }
        // 如果设置为默认模型，取消该 provider 下其他默认模型
        if (Boolean.TRUE.equals(request.getDefaultModel()) && !Boolean.TRUE.equals(model.getDefaultModel())) {
            clearDefaultModel(model.getProviderId());
        }
        BeanUtils.copyProperties(request, model);
        updateById(model);
    }

    @Override
    public void deleteModel(Long id) {
        ModelConfig model = getById(id);
        if (model == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "数据不存在");
        }
        removeById(id);
    }

    private void clearDefaultModel(Long providerId) {
        LambdaQueryWrapper<ModelConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ModelConfig::getProviderId, providerId).eq(ModelConfig::getDefaultModel, true);
        List<ModelConfig> defaults = list(wrapper);
        if (!defaults.isEmpty()) {
            defaults.forEach(m -> m.setDefaultModel(false));
            updateBatchById(defaults);
        }
    }

    private ModelConfigVO toVO(ModelConfig model) {
        ModelConfigVO vo = new ModelConfigVO();
        BeanUtils.copyProperties(model, vo);
        return vo;
    }

    // ========== ModelConfigApi 实现 ==========

    @Override
    @Transactional(readOnly = true)
    public ModelConfigDTO getModelByModelId(String modelId) {
        LambdaQueryWrapper<ModelConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ModelConfig::getModelId, modelId).eq(ModelConfig::getEnabled, true);
        ModelConfig model = getOne(wrapper);
        return model != null ? toDTO(model) : null;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ModelConfigDTO> listEnabledModelsByProviderId(Long providerId) {
        LambdaQueryWrapper<ModelConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ModelConfig::getProviderId, providerId)
                .eq(ModelConfig::getEnabled, true)
                .orderByAsc(ModelConfig::getSortOrder)
                .orderByAsc(ModelConfig::getId);
        return list(wrapper).stream().map(this::toDTO).toList();
    }

    private ModelConfigDTO toDTO(ModelConfig model) {
        ModelConfigDTO dto = new ModelConfigDTO();
        BeanUtils.copyProperties(model, dto);
        return dto;
    }
}
