package com.hify.chat.mapper.trace;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hify.chat.entity.trace.TraceWorkflowNodeExecution;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工作流节点执行 Mapper（chat 模块只读）
 */
@Mapper
public interface ChatTraceWorkflowNodeExecutionMapper extends BaseMapper<TraceWorkflowNodeExecution> {
}
