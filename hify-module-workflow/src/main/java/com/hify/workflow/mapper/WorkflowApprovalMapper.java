package com.hify.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hify.workflow.entity.WorkflowApproval;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工作流审批记录 Mapper
 */
@Mapper
public interface WorkflowApprovalMapper extends BaseMapper<WorkflowApproval> {
}
