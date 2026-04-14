package com.hify.model.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hify.common.web.entity.PageParam;
import com.hify.common.web.entity.PageResult;
import com.hify.model.dto.ModelProviderCreateRequest;
import com.hify.model.dto.ModelProviderUpdateRequest;
import com.hify.model.entity.ModelProvider;
import com.hify.model.entity.ModelProviderVO;
import com.hify.model.mapper.ModelProviderMapper;
import com.hify.model.service.ModelProviderService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 模型提供商 Service 实现
 *
 * @author hify
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class ModelProviderServiceImpl extends ServiceImpl<ModelProviderMapper, ModelProvider> implements ModelProviderService {

    @Override
    @Transactional(readOnly = true)
    public PageResult<ModelProviderVO> pageProviders(PageParam pageParam) {
        return PageResult.of(baseMapper.selectPage(pageParam.toPage(ModelProvider.class), null)
                .convert(this::toVO));
    }

    @Override
    @Transactional(readOnly = true)
    public ModelProviderVO getProviderDetail(Long id) {
        ModelProvider provider = getById(id);
        return provider != null ? toVO(provider) : null;
    }

    @Override
    public Long createProvider(ModelProviderCreateRequest request) {
        ModelProvider provider = new ModelProvider();
        BeanUtils.copyProperties(request, provider);
        save(provider);
        return provider.getId();
    }

    @Override
    public void updateProvider(Long id, ModelProviderUpdateRequest request) {
        ModelProvider provider = getById(id);
        if (provider == null) {
            throw new IllegalArgumentException("数据不存在");
        }
        BeanUtils.copyProperties(request, provider);
        updateById(provider);
    }

    @Override
    public void deleteProvider(Long id) {
        ModelProvider provider = getById(id);
        if (provider == null) {
            throw new IllegalArgumentException("数据不存在");
        }
        removeById(id);
    }

    private ModelProviderVO toVO(ModelProvider provider) {
        ModelProviderVO vo = new ModelProviderVO();
        BeanUtils.copyProperties(provider, vo);
        return vo;
    }
}
