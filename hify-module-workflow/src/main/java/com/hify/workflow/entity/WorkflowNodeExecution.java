package com.hify.workflow.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hify.common.web.entity.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 工作流节点执行记录
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("workflow_node_execution")
public class WorkflowNodeExecution extends BaseEntity {

    /**
     * 实例 ID
     */
    private Long executionId;

    /**
     * 节点 ID
     */
    private String nodeId;

    /**
     * 节点类型
     */
    private String nodeType;

    /**
     * 执行状态：running / completed / failed
     */
    private String status;

    /**
     * 输入参数（JSON）
     */
    private String inputJson;

    /**
     * 输出结果（JSON）
     */
    private String outputJson;

    /**
     * 错误信息
     */
    private String errorMsg;

    /**
     * 开始时间
     */
    private LocalDateTime startedAt;

    /**
     * 结束时间
     */
    private LocalDateTime endedAt;
}
