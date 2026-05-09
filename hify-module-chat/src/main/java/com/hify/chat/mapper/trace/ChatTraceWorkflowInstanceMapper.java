package com.hify.chat.mapper.trace;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hify.chat.entity.trace.TraceWorkflowInstance;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工作流实例 Mapper（chat 模块只读）
 */
@Mapper
public interface ChatTraceWorkflowInstanceMapper extends BaseMapper<TraceWorkflowInstance> {
}
