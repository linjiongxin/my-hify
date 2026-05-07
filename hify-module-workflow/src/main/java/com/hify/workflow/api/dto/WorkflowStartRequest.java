package com.hify.workflow.api.dto;

import lombok.Data;

import java.util.Map;

/**
 * 启动工作流请求
 */
@Data
public class WorkflowStartRequest {

    /**
     * 工作流 ID
     */
    private Long workflowId;

    /**
     * 输入参数（Map<String, Object>）
     */
    private Map<String, Object> inputs;
}