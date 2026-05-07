package com.hify.workflow.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.hify.common.web.entity.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 工作流审批记录实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("workflow_approval")
public class WorkflowApproval extends BaseEntity {

    /**
     * 实例 ID
     */
    private Long instanceId;

    /**
     * 节点 ID
     */
    private String nodeId;

    /**
     * 状态：pending, approved, rejected
     */
    private String status;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 处理时间
     */
    private LocalDateTime processedAt;
}
