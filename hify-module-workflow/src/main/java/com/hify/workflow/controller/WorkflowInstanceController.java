package com.hify.workflow.controller;

import com.hify.common.core.enums.ResultCode;
import com.hify.common.core.exception.BizException;
import com.hify.workflow.api.WorkflowApi;
import com.hify.workflow.api.dto.WorkflowApprovalDTO;
import com.hify.workflow.api.dto.WorkflowInstanceDTO;
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
@RequestMapping("/workflow/instance")
@RequiredArgsConstructor
public class WorkflowInstanceController {

    private final WorkflowApi workflowApi;

    @PostMapping
    public String start(@RequestBody @Validated WorkflowStartRequest dto) {
        log.info("触发工作流执行: workflowId={}", dto.getWorkflowId());
        return workflowApi.start(dto);
    }

    @GetMapping("/{instanceId}")
    public WorkflowInstanceDTO getInstanceById(@PathVariable("instanceId") Long instanceId) {
        WorkflowInstanceDTO dto = workflowApi.getInstanceById(instanceId);
        if (dto == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "工作流实例不存在");
        }
        return dto;
    }

    @GetMapping("/{instanceId}/pending-approvals")
    public List<WorkflowApprovalDTO> getPendingApprovals(@PathVariable("instanceId") Long instanceId) {
        return workflowApi.getPendingApprovals(instanceId);
    }
}