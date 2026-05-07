package com.hify.workflow.controller;

import com.hify.common.core.enums.ResultCode;
import com.hify.common.core.exception.BizException;
import com.hify.common.web.entity.PageResult;
import com.hify.workflow.api.WorkflowApi;
import com.hify.workflow.api.dto.WorkflowCreateRequest;
import com.hify.workflow.api.dto.WorkflowDTO;
import com.hify.workflow.api.dto.WorkflowEdgeDTO;
import com.hify.workflow.api.dto.WorkflowNodeDTO;
import com.hify.workflow.api.dto.WorkflowQueryDTO;
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
    public Long create(@RequestBody @Validated WorkflowCreateRequest dto) {
        log.info("创建工作流: {}", dto.getName());
        return workflowApi.create(dto);
    }

    @GetMapping
    public PageResult<WorkflowDTO> list(@ModelAttribute WorkflowQueryDTO query) {
        return workflowApi.list(query);
    }

    @GetMapping("/{id}")
    public WorkflowDTO getById(@PathVariable("id") Long id) {
        WorkflowDTO dto = workflowApi.getById(id);
        if (dto == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "工作流不存在");
        }
        return dto;
    }

    @PutMapping("/{id}")
    public void update(@PathVariable("id") Long id, @RequestBody @Validated WorkflowUpdateRequest dto) {
        log.info("更新工作流: {}", id);
        workflowApi.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable("id") Long id) {
        log.info("删除工作流: {}", id);
        workflowApi.delete(id);
    }

    @GetMapping("/{id}/nodes")
    public List<WorkflowNodeDTO> getNodes(@PathVariable("id") Long id) {
        return workflowApi.getNodes(id);
    }

    @GetMapping("/{id}/edges")
    public List<WorkflowEdgeDTO> getEdges(@PathVariable("id") Long id) {
        return workflowApi.getEdges(id);
    }
}