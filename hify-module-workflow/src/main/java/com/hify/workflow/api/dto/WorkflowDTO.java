package com.hify.workflow.api.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工作流响应 DTO
 */
@Data
public class WorkflowDTO {

    private Long id;

    /**
     * 工作流名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;

    /**
     * 状态：draft, published, disabled
     */
    private String status;

    /**
     * 版本号
     */
    private Integer version;

    /**
     * 重试配置（JSON）
     */
    private String retryConfig;

    /**
     * 工作流配置（JSON）
     */
    private String config;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}