package com.hify.workflow.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hify.common.web.entity.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工作流定义实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("workflow")
public class Workflow extends BaseEntity {

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
}
