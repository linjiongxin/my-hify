package com.hify.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hify.workflow.entity.WorkflowEdge;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工作流连线定义 Mapper
 */
@Mapper
public interface WorkflowEdgeMapper extends BaseMapper<WorkflowEdge> {
}
