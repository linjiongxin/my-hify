package com.hify.workflow.controller;

import com.hify.common.core.enums.ResultCode;
import com.hify.common.core.exception.BizException;
import com.hify.common.web.entity.Result;
import com.hify.workflow.api.WorkflowApi;
import com.hify.common.web.entity.PageResult;
import com.hify.workflow.api.WorkflowApi;
import com.hify.workflow.api.dto.WorkflowApprovalDTO;
import com.hify.workflow.api.dto.WorkflowInstanceDTO;
import com.hify.workflow.api.dto.WorkflowNodeExecutionDTO;
import com.hify.workflow.api.dto.WorkflowStartRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 工作流实例 Controller
 */
@Slf4j
@RestController
@RequestMapping("/workflow/instances")
@RequiredArgsConstructor
public class WorkflowInstanceController {

    private final WorkflowApi workflowApi;

    @PostMapping
    public Result<String> start(@RequestBody @Validated WorkflowStartRequest dto) {
        log.info("触发工作流执行: workflowId={}", dto.getWorkflowId());
        return Result.success(workflowApi.start(dto));
    }

    @GetMapping
    public Result<PageResult<WorkflowInstanceDTO>> listInstances(
            @RequestParam(name = "workflowId", required = false) Long workflowId,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", defaultValue = "1") Integer page,
            @RequestParam(name = "pageSize", defaultValue = "20") Integer pageSize
    ) {
        WorkflowApi.InstanceQueryDTO query = new WorkflowApi.InstanceQueryDTO();
        query.setWorkflowId(workflowId);
        query.setStatus(status);
        query.setPage(page);
        query.setPageSize(pageSize);
        return Result.success(workflowApi.listInstances(query));
    }

    @GetMapping("/{instanceId}")
    public Result<WorkflowInstanceDTO> getInstanceById(@PathVariable("instanceId") Long instanceId) {
        WorkflowInstanceDTO dto = workflowApi.getInstanceById(instanceId);
        if (dto == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "工作流实例不存在");
        }
        return Result.success(dto);
    }

    @GetMapping("/{instanceId}/pending-approvals")
    public Result<List<WorkflowApprovalDTO>> getPendingApprovals(@PathVariable("instanceId") Long instanceId) {
        return Result.success(workflowApi.getPendingApprovals(instanceId));
    }

    @GetMapping("/{instanceId}/node-executions")
    public Result<List<WorkflowNodeExecutionDTO>> getNodeExecutions(@PathVariable("instanceId") Long instanceId) {
        return Result.success(workflowApi.getNodeExecutions(instanceId));
    }
}
