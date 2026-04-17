package com.hify.model.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hify.common.web.entity.PageParam;
import com.hify.common.web.entity.PageResult;
import com.hify.common.core.exception.BizException;
import com.hify.model.dto.ModelCreateRequest;
import com.hify.model.dto.ModelUpdateRequest;
import com.hify.model.entity.Model;
import com.hify.model.entity.ModelVO;
import com.hify.model.mapper.ModelMapper;
import com.hify.model.service.ModelService;
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
public class ModelServiceImpl extends ServiceImpl<ModelMapper, Model> implements ModelService {

    @Override
    @Transactional(readOnly = true)
    public PageResult<ModelVO> pageModels(PageParam pageParam) {
        LambdaQueryWrapper<Model> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(Model::getSortOrder).orderByAsc(Model::getId);
        return PageResult.of(baseMapper.selectPage(pageParam.toPage(Model.class), wrapper)
                .convert(this::toVO));
    }

    @Override
    @Transactional(readOnly = true)
    public ModelVO getModelDetail(Long id) {
        Model model = getById(id);
        return model != null ? toVO(model) : null;
    }

    @Override
    public Long createModel(ModelCreateRequest request) {
        // 如果设置为默认模型，取消该 provider 下其他默认模型
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            clearDefaultModel(request.getProviderId());
        }
        Model model = new Model();
        BeanUtils.copyProperties(request, model);
        save(model);
        return model.getId();
    }

    @Override
    public void updateModel(Long id, ModelUpdateRequest request) {
        Model model = getById(id);
        if (model == null) {
            throw new BizException("数据不存在");
        }
        // 如果设置为默认模型，取消该 provider 下其他默认模型
        if (Boolean.TRUE.equals(request.getIsDefault()) && !Boolean.TRUE.equals(model.getIsDefault())) {
            clearDefaultModel(model.getProviderId());
        }
        BeanUtils.copyProperties(request, model);
        updateById(model);
    }

    @Override
    public void deleteModel(Long id) {
        Model model = getById(id);
        if (model == null) {
            throw new BizException("数据不存在");
        }
        removeById(id);
    }

    private void clearDefaultModel(Long providerId) {
        LambdaQueryWrapper<Model> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Model::getProviderId, providerId).eq(Model::getIsDefault, true);
        List<Model> defaults = list(wrapper);
        for (Model m : defaults) {
            m.setIsDefault(false);
            updateById(m);
        }
    }

    private ModelVO toVO(Model model) {
        ModelVO vo = new ModelVO();
        BeanUtils.copyProperties(model, vo);
        return vo;
    }
}
