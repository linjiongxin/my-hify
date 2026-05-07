package com.hify.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hify.workflow.entity.WorkflowNode;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工作流节点定义 Mapper
 */
@Mapper
public interface WorkflowNodeMapper extends BaseMapper<WorkflowNode> {
}
