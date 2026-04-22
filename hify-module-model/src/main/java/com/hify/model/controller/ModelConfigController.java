package com.hify.model.controller;

import com.hify.common.web.entity.PageParam;
import com.hify.common.web.entity.PageResult;
import com.hify.common.web.entity.Result;
import com.hify.model.dto.ModelConfigCreateRequest;
import com.hify.model.dto.ModelConfigUpdateRequest;
import com.hify.model.vo.ModelConfigVO;
import com.hify.model.service.ModelConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 模型控制器
 *
 * @author hify
 */
@RestController
@RequestMapping("/model")
@RequiredArgsConstructor
@Validated
public class ModelConfigController {

    private final ModelConfigService modelService;

    /**
     * 分页列表
     */
    @GetMapping("/page")
    public Result<PageResult<ModelConfigVO>> page(@Valid PageParam pageParam) {
        return Result.success(modelService.pageModels(pageParam));
    }

    /**
     * 获取所有启用的模型（用于下拉选择）
     */
    @GetMapping("/all")
    public Result<java.util.List<ModelConfigVO>> listAllEnabled() {
        return Result.success(modelService.listAllEnabledModels());
    }

    /**
     * 详情
     */
    @GetMapping("/{id}")
    public Result<ModelConfigVO> detail(@PathVariable("id") Long id) {
        return Result.success(modelService.getModelDetail(id));
    }

    /**
     * 创建
     */
    @PostMapping
    public Result<Long> create(@Valid @RequestBody ModelConfigCreateRequest request) {
        return Result.success(modelService.createModel(request));
    }

    /**
     * 更新
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable("id") Long id, @Valid @RequestBody ModelConfigUpdateRequest request) {
        modelService.updateModel(id, request);
        return Result.success();
    }

    /**
     * 删除
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        modelService.deleteModel(id);
        return Result.success();
    }
}
