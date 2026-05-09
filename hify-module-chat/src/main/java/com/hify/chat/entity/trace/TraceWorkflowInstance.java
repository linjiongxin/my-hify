package com.hify.chat.entity.trace;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.hify.common.web.entity.base.BaseEntity;
import com.hify.common.web.handler.JsonbStringTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 工作流执行实例（chat 模块只读视图）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("workflow_instance")
public class TraceWorkflowInstance extends BaseEntity {

    private Long workflowId;
    private String status;
    private String currentNodeId;

    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String context;

    private String errorMsg;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private String traceId;
}
