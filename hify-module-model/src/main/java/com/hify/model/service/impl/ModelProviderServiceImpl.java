package com.hify.model.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hify.common.web.entity.PageParam;
import com.hify.common.web.entity.PageResult;
import com.hify.common.core.exception.BizException;
import com.hify.model.dto.ModelProviderCreateRequest;
import com.hify.model.dto.ModelProviderUpdateRequest;
import com.hify.model.entity.ModelProvider;
import com.hify.model.entity.ModelProviderStatus;
import com.hify.model.entity.ModelProviderVO;
import com.hify.model.mapper.ModelProviderMapper;
import com.hify.model.service.ModelProviderService;
import com.hify.model.service.ModelProviderStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 模型提供商 Service 实现
 *
 * @author hify
 */
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class ModelProviderServiceImpl extends ServiceImpl<ModelProviderMapper, ModelProvider> implements ModelProviderService {

    private final ModelProviderStatusService modelProviderStatusService;

    @Override
    @Transactional(readOnly = true)
    public PageResult<ModelProviderVO> pageProviders(PageParam pageParam) {
        return PageResult.of(baseMapper.selectProviderPage(pageParam.toPage(ModelProviderVO.class), false));
    }

    @Override
    @Transactional(readOnly = true)
    public ModelProviderVO getProviderDetail(Long id) {
        return baseMapper.selectProviderDetail(id);
    }

    @Override
    public Long createProvider(ModelProviderCreateRequest request) {
        ModelProvider provider = new ModelProvider();
        BeanUtils.copyProperties(request, provider);
        save(provider);

        // 同步初始化健康状态记录
        ModelProviderStatus status = new ModelProviderStatus();
        status.setProviderId(provider.getId());
        status.setHealthStatus("unknown");
        modelProviderStatusService.save(status);

        return provider.getId();
    }

    @Override
    public void updateProvider(Long id, ModelProviderUpdateRequest request) {
        ModelProvider provider = getById(id);
        if (provider == null) {
            throw new BizException("数据不存在");
        }
        BeanUtils.copyProperties(request, provider);
        updateById(provider);
    }

    @Override
    public void deleteProvider(Long id) {
        ModelProvider provider = getById(id);
        if (provider == null) {
            throw new BizException("数据不存在");
        }
        removeById(id);
        // 级联删除健康状态记录（数据库也有 ON DELETE CASCADE，此处做兜底）
        modelProviderStatusService.removeById(id);
    }
}
