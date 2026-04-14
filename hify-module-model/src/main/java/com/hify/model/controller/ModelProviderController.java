package com.hify.model.controller;

import com.hify.common.web.entity.PageParam;
import com.hify.common.web.entity.PageResult;
import com.hify.common.web.entity.Result;
import com.hify.model.dto.ModelProviderCreateRequest;
import com.hify.model.dto.ModelProviderUpdateRequest;
import com.hify.model.entity.ModelProvider;
import com.hify.model.service.ModelProviderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 模型提供商控制器
 *
 * @author hify
 */
@RestController
@RequestMapping("/model-provider")
@RequiredArgsConstructor
public class ModelProviderController {

    private final ModelProviderService modelProviderService;

    /**
     * 分页列表
     */
    @GetMapping("/page")
    public Result<PageResult<ModelProvider>> page(PageParam pageParam) {
        return Result.success(PageResult.of(modelProviderService.page(pageParam.toPage(ModelProvider.class))));
    }

    /**
     * 详情
     */
    @GetMapping("/{id}")
    public Result<ModelProvider> detail(@PathVariable("id") Long id) {
        ModelProvider provider = modelProviderService.getById(id);
        if (provider == null) {
            return Result.error(404, "数据不存在");
        }
        return Result.success(provider);
    }

    /**
     * 创建
     */
    @PostMapping
    public Result<Long> create(@Valid @RequestBody ModelProviderCreateRequest request) {
        ModelProvider provider = new ModelProvider();
        provider.setName(request.getName());
        provider.setCode(request.getCode());
        provider.setApiBaseUrl(request.getApiBaseUrl());
        provider.setApiKeyRequired(request.getApiKeyRequired());
        provider.setEnabled(request.getEnabled());
        provider.setSortOrder(request.getSortOrder());
        modelProviderService.save(provider);
        return Result.success(provider.getId());
    }

    /**
     * 更新
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable("id") Long id, @Valid @RequestBody ModelProviderUpdateRequest request) {
        ModelProvider provider = modelProviderService.getById(id);
        if (provider == null) {
            return Result.error(404, "数据不存在");
        }
        provider.setName(request.getName());
        provider.setApiBaseUrl(request.getApiBaseUrl());
        provider.setApiKeyRequired(request.getApiKeyRequired());
        provider.setEnabled(request.getEnabled());
        provider.setSortOrder(request.getSortOrder());
        modelProviderService.updateById(provider);
        return Result.success();
    }

    /**
     * 删除
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        modelProviderService.removeById(id);
        return Result.success();
    }
}
