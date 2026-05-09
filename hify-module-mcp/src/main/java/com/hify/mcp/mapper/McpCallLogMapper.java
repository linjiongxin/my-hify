package com.hify.mcp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hify.mcp.entity.McpCallLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * MCP 调用日志 Mapper
 */
@Mapper
public interface McpCallLogMapper extends BaseMapper<McpCallLog> {
}
