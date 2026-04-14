package com.hify.model.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hify.model.entity.ModelProvider;
import com.hify.model.mapper.ModelProviderMapper;
import com.hify.model.service.ModelProviderService;
import org.springframework.stereotype.Service;

/**
 * 模型提供商 Service 实现
 *
 * @author hify
 */
@Service
public class ModelProviderServiceImpl extends ServiceImpl<ModelProviderMapper, ModelProvider> implements ModelProviderService {
}
