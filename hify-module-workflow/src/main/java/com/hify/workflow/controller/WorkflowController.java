package com.hify.workflow.controller;

import com.hify.common.core.enums.ResultCode;
import com.hify.common.core.exception.BizException;
import com.hify.common.web.entity.PageResult;
import com.hify.common.web.entity.Result;
import com.hify.workflow.api.WorkflowApi;
import com.hify.workflow.api.dto.WorkflowCreateRequest;
import com.hify.workflow.api.dto.WorkflowDTO;
import com.hify.workflow.api.dto.WorkflowEdgeDTO;
import com.hify.workflow.api.dto.WorkflowNodeDTO;
import com.hify.workflow.api.dto.WorkflowUpdateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 工作流管理 Controller
 */
@Slf4j
@RestController
@RequestMapping("/workflow")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowApi workflowApi;

    @PostMapping
    public Result<Long> create(@RequestBody @Validated WorkflowCreateRequest dto) {
        log.info("创建工作流: {}", dto.getName());
        return Result.success(workflowApi.create(dto));
    }

    @GetMapping
    public Result<PageResult<WorkflowDTO>> list(@ModelAttribute WorkflowApi.WorkflowQueryDTO query) {
        return Result.success(workflowApi.list(query));
    }

    @GetMapping("/{id}")
    public Result<WorkflowDTO> getById(@PathVariable("id") Long id) {
        WorkflowDTO dto = workflowApi.getById(id);
        if (dto == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "工作流不存在");
        }
        return Result.success(dto);
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable("id") Long id, @RequestBody @Validated WorkflowUpdateRequest dto) {
        log.info("更新工作流: {}", id);
        workflowApi.update(id, dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        log.info("删除工作流: {}", id);
        workflowApi.delete(id);
        return Result.success();
    }

    @GetMapping("/{id}/nodes")
    public Result<List<WorkflowNodeDTO>> getNodes(@PathVariable("id") Long id) {
        return Result.success(workflowApi.getNodes(id));
    }

    @GetMapping("/{id}/edges")
    public Result<List<WorkflowEdgeDTO>> getEdges(@PathVariable("id") Long id) {
        return Result.success(workflowApi.getEdges(id));
    }

    @PutMapping("/{id}/nodes")
    public Result<List<WorkflowNodeDTO>> saveNodes(@PathVariable("id") Long id, @RequestBody List<WorkflowNodeDTO> nodes) {
        log.info("保存工作流节点: workflowId={}, nodeCount={}", id, nodes.size());
        return Result.success(workflowApi.saveNodes(id, nodes));
    }

    @PutMapping("/{id}/edges")
    public Result<List<WorkflowEdgeDTO>> saveEdges(@PathVariable("id") Long id, @RequestBody List<WorkflowEdgeDTO> edges) {
        log.info("保存工作流连线: workflowId={}, edgeCount={}", id, edges.size());
        return Result.success(workflowApi.saveEdges(id, edges));
    }
}