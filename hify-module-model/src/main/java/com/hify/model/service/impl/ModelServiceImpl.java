package com.hify.model.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hify.model.entity.Model;
import com.hify.model.mapper.ModelMapper;
import com.hify.model.service.ModelService;
import org.springframework.stereotype.Service;

/**
 * 模型 Service 实现
 *
 * @author hify
 */
@Service
public class ModelServiceImpl extends ServiceImpl<ModelMapper, Model> implements ModelService {
}
