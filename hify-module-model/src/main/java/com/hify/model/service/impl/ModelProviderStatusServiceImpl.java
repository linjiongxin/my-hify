package com.hify.model.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hify.model.entity.ModelProviderStatus;
import com.hify.model.mapper.ModelProviderStatusMapper;
import com.hify.model.service.ModelProviderStatusService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 模型提供商状态 Service 实现
 *
 * @author hify
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class ModelProviderStatusServiceImpl extends ServiceImpl<ModelProviderStatusMapper, ModelProviderStatus> implements ModelProviderStatusService {
}
