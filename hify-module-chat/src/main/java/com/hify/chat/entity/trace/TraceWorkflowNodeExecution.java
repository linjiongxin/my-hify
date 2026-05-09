package com.hify.chat.entity.trace;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.hify.common.web.entity.base.BaseEntity;
import com.hify.common.web.handler.JsonbStringTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 工作流节点执行记录（chat 模块只读视图）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("workflow_node_execution")
public class TraceWorkflowNodeExecution extends BaseEntity {

    private Long executionId;
    private String nodeId;
    private String nodeType;
    private String status;

    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String inputJson;

    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String outputJson;

    private String errorMsg;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private String traceId;
}
