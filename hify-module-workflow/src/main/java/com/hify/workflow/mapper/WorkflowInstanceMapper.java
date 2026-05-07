package com.hify.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hify.workflow.entity.WorkflowInstance;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工作流执行实例 Mapper
 */
@Mapper
public interface WorkflowInstanceMapper extends BaseMapper<WorkflowInstance> {
}
