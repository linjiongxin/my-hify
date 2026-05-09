package com.hify.chat.entity.trace;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.hify.common.web.entity.base.BaseEntity;
import com.hify.common.web.handler.JsonbStringTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * MCP 调用日志（chat 模块只读视图）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mcp_call_log")
public class TraceMcpCallLog extends BaseEntity {

    private String serverUrl;
    private String toolName;

    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String requestJson;

    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String responseJson;

    private String status;
    private Integer durationMs;
    private String errorMsg;
    private String traceId;
}
