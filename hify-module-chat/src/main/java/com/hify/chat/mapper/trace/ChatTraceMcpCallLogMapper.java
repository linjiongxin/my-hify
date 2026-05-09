package com.hify.chat.mapper.trace;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hify.chat.entity.trace.TraceMcpCallLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * MCP 调用日志 Mapper（chat 模块只读）
 */
@Mapper
public interface ChatTraceMcpCallLogMapper extends BaseMapper<TraceMcpCallLog> {
}
