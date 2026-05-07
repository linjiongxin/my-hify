package com.hify.workflow.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hify.common.web.entity.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工作流节点定义实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("workflow_node")
public class WorkflowNode extends BaseEntity {

    /**
     * 工作流 ID
     */
    private Long workflowId;

    /**
     * 节点唯一标识
     */
    private String nodeId;

    /**
     * 节点类型
     */
    private String type;

    /**
     * 节点名称
     */
    private String name;

    /**
     * 节点配置（JSON）
     */
    private String config;

    /**
     * 节点 X 坐标
     */
    private Integer positionX;

    /**
     * 节点 Y 坐标
     */
    private Integer positionY;

    /**
     * 重试配置（JSON）
     */
    private String retryConfig;
}
