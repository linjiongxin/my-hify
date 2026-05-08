package com.hify.workflow.controller;

import com.hify.common.web.entity.Result;
import com.hify.workflow.api.WorkflowApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 工作流审批 Controller
 */
@Slf4j
@RestController
@RequestMapping("/workflow/approval")
@RequiredArgsConstructor
public class WorkflowApprovalController {

    private final WorkflowApi workflowApi;

    @PostMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable("id") Long id, @RequestParam(name = "remark", required = false) String remark) {
        log.info("审批通过: approvalId={}, remark={}", id, remark);
        workflowApi.approve(id, remark);
        return Result.success();
    }

    @PostMapping("/{id}/reject")
    public Result<Void> reject(@PathVariable("id") Long id, @RequestParam(name = "remark", required = false) String remark) {
        log.info("审批拒绝: approvalId={}, remark={}", id, remark);
        workflowApi.reject(id, remark);
        return Result.success();
    }
}
