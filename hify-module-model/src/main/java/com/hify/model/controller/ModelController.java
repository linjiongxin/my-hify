package com.hify.model.controller;

import com.hify.common.web.entity.PageParam;
import com.hify.common.web.entity.PageResult;
import com.hify.common.web.entity.Result;
import com.hify.model.dto.ModelCreateRequest;
import com.hify.model.dto.ModelUpdateRequest;
import com.hify.model.entity.ModelVO;
import com.hify.model.service.ModelService;
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
public class ModelController {

    private final ModelService modelService;

    /**
     * 分页列表
     */
    @GetMapping("/page")
    public Result<PageResult<ModelVO>> page(@Valid PageParam pageParam) {
        return Result.success(modelService.pageModels(pageParam));
    }

    /**
     * 详情
     */
    @GetMapping("/{id}")
    public Result<ModelVO> detail(@PathVariable("id") Long id) {
        ModelVO vo = modelService.getModelDetail(id);
        if (vo == null) {
            return Result.error(404, "数据不存在");
        }
        return Result.success(vo);
    }

    /**
     * 创建
     */
    @PostMapping
    public Result<Long> create(@Valid @RequestBody ModelCreateRequest request) {
        return Result.success(modelService.createModel(request));
    }

    /**
     * 更新
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable("id") Long id, @Valid @RequestBody ModelUpdateRequest request) {
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
