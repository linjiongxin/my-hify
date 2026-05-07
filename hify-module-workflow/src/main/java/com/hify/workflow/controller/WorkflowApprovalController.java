package com.hify.workflow.controller;

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

    private final com.hify.workflow.api.WorkflowApi workflowApi;

    @PostMapping("/{id}/approve")
    public void approve(@PathVariable("id") Long id, @RequestParam(name = "remark", required = false) String remark) {
        log.info("审批通过: approvalId={}, remark={}", id, remark);
        workflowApi.approve(id, remark);
    }

    @PostMapping("/{id}/reject")
    public void reject(@PathVariable("id") Long id, @RequestParam(name = "remark", required = false) String remark) {
        log.info("审批拒绝: approvalId={}, remark={}", id, remark);
        workflowApi.reject(id, remark);
    }
}