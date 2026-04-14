package com.hify.model.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hify.model.entity.Model;
import com.hify.model.mapper.ModelMapper;
import com.hify.model.service.ModelService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 模型 Service 实现
 *
 * @author hify
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class ModelServiceImpl extends ServiceImpl<ModelMapper, Model> implements ModelService {
}
