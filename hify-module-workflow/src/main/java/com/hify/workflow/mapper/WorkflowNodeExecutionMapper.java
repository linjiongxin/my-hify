package com.hify.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hify.workflow.entity.WorkflowNodeExecution;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工作流节点执行记录 Mapper
 */
@Mapper
public interface WorkflowNodeExecutionMapper extends BaseMapper<WorkflowNodeExecution> {
}
