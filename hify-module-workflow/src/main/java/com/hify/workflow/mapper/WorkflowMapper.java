package com.hify.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hify.workflow.entity.Workflow;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工作流定义 Mapper
 */
@Mapper
public interface WorkflowMapper extends BaseMapper<Workflow> {
}
