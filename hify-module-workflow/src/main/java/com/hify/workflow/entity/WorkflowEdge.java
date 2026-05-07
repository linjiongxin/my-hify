package com.hify.workflow.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hify.common.web.entity.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工作流连线定义实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("workflow_edge")
public class WorkflowEdge extends BaseEntity {

    /**
     * 工作流 ID
     */
    private Long workflowId;

    /**
     * 源节点 ID
     */
    private String sourceNode;

    /**
     * 目标节点 ID
     */
    private String targetNode;

    /**
     * 条件表达式
     */
    private String condition;

    /**
     * 连线顺序
     */
    private Integer edgeIndex;
}
