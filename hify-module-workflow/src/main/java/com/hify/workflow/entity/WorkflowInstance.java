package com.hify.workflow.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.hify.common.web.entity.base.BaseEntity;
import com.hify.common.web.handler.JsonbStringTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 工作流执行实例实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("workflow_instance")
public class WorkflowInstance extends BaseEntity {

    /**
     * 工作流 ID
     */
    private Long workflowId;

    /**
     * 状态：running, completed, failed, cancelled
     */
    private String status;

    /**
     * 当前节点 ID
     */
    private String currentNodeId;

    /**
     * 执行上下文（JSON）
     */
    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String context;

    /**
     * 错误信息
     */
    private String errorMsg;

    /**
     * 开始时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime startedAt;

    /**
     * 结束时间
     */
    private LocalDateTime finishedAt;
}
