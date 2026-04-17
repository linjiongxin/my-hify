package com.hify.model.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hify.common.web.entity.PageParam;
import com.hify.common.web.entity.PageResult;
import com.hify.common.core.enums.ResultCode;
import com.hify.common.core.exception.BizException;
import com.hify.model.dto.ModelProviderCreateRequest;
import com.hify.model.dto.ModelProviderUpdateRequest;
import com.hify.model.entity.ModelProvider;
import com.hify.model.entity.ModelProviderStatus;
import com.hify.model.vo.ModelProviderVO;
import com.hify.model.api.dto.ModelProviderDTO;
import com.hify.model.constant.ModelConstants;
import com.hify.model.mapper.ModelProviderMapper;
import com.hify.model.api.ModelProviderApi;
import com.hify.model.service.ModelProviderService;
import com.hify.model.service.ModelProviderStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 模型提供商 Service 实现
 *
 * @author hify
 */
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class ModelProviderServiceImpl extends ServiceImpl<ModelProviderMapper, ModelProvider> implements ModelProviderService, ModelProviderApi {

    private final ModelProviderStatusService modelProviderStatusService;

    @Override
    @Transactional(readOnly = true)
    public PageResult<ModelProviderVO> pageProviders(PageParam pageParam) {
        return PageResult.of(baseMapper.selectProviderPage(pageParam.toPage(ModelProviderVO.class), false));
    }

    @Override
    @Transactional(readOnly = true)
    public ModelProviderVO getProviderDetail(Long id) {
        ModelProviderVO vo = baseMapper.selectProviderDetail(id);
        if (vo == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "数据不存在");
        }
        return vo;
    }

    @Override
    public Long createProvider(ModelProviderCreateRequest request) {
        ModelProvider provider = new ModelProvider();
        BeanUtils.copyProperties(request, provider);
        save(provider);

        // 同步初始化健康状态记录
        ModelProviderStatus status = new ModelProviderStatus();
        status.setProviderId(provider.getId());
        status.setHealthStatus(ModelConstants.HealthStatus.UNKNOWN);
        modelProviderStatusService.save(status);

        return provider.getId();
    }

    @Override
    public void updateProvider(Long id, ModelProviderUpdateRequest request) {
        ModelProvider provider = getById(id);
        if (provider == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "数据不存在");
        }
        BeanUtils.copyProperties(request, provider);
        updateById(provider);
    }

    @Override
    public void deleteProvider(Long id) {
        ModelProvider provider = getById(id);
        if (provider == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "数据不存在");
        }
        removeById(id);
        // 级联删除健康状态记录（数据库也有 ON DELETE CASCADE，此处做兜底）
        modelProviderStatusService.removeById(id);
    }

    // ========== ModelProviderApi 实现 ==========

    @Override
    @Transactional(readOnly = true)
    public List<ModelProviderDTO> listEnabledProviders() {
        List<ModelProvider> providers = lambdaQuery()
                .eq(ModelProvider::getEnabled, true)
                .list();
        if (providers.isEmpty()) {
            return List.of();
        }
        List<Long> providerIds = providers.stream()
                .map(ModelProvider::getId)
                .toList();
        Map<Long, String> healthMap = modelProviderStatusService.lambdaQuery()
                .in(ModelProviderStatus::getProviderId, providerIds)
                .list()
                .stream()
                .collect(Collectors.toMap(ModelProviderStatus::getProviderId, ModelProviderStatus::getHealthStatus));
        return providers.stream()
                .map(p -> {
                    ModelProviderDTO dto = toDto(p);
                    dto.setHealthStatus(healthMap.getOrDefault(p.getId(), ModelConstants.HealthStatus.UNKNOWN));
                    return dto;
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ModelProviderDTO getProviderById(Long id) {
        ModelProviderVO vo = baseMapper.selectProviderDetail(id);
        if (vo == null) {
            return null;
        }
        ModelProviderDTO dto = new ModelProviderDTO();
        BeanUtils.copyProperties(vo, dto);
        return dto;
    }

    private ModelProviderDTO toDto(ModelProvider provider) {
        ModelProviderDTO dto = new ModelProviderDTO();
        BeanUtils.copyProperties(provider, dto);
        return dto;
    }
}
