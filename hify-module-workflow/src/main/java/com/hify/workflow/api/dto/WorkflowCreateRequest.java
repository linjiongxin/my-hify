package com.hify.workflow.api.dto;

import lombok.Data;

/**
 * 创建工作流请求
 */
@Data
public class WorkflowCreateRequest {

    /**
     * 工作流名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;

    /**
     * 重试配置（JSON）
     */
    private String retryConfig;

    /**
     * 工作流配置（JSON）
     */
    private String config;
}