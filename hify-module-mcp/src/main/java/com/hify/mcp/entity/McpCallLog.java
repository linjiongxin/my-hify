package com.hify.mcp.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.hify.common.web.entity.base.BaseEntity;
import com.hify.common.web.handler.JsonbStringTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * MCP 调用日志
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mcp_call_log")
public class McpCallLog extends BaseEntity {

    /**
     * MCP Server 地址
     */
    private String serverUrl;

    /**
     * 工具名称
     */
    private String toolName;

    /**
     * 请求参数 JSON
     */
    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String requestJson;

    /**
     * 响应结果 JSON
     */
    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String responseJson;

    /**
     * 调用状态：success / failed
     */
    private String status;

    /**
     * 耗时（毫秒）
     */
    private Integer durationMs;

    /**
     * 错误信息
     */
    private String errorMsg;

    /**
     * 链路追踪 ID
     */
    private String traceId;
}
