package com.hify.mcp.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.hify.common.web.entity.base.BaseEntity;
import com.hify.common.web.handler.JsonbTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * MCP Server 实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mcp_server")
public class McpServer extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 名称
     */
    private String name;

    /**
     * 编码（唯一）
     */
    private String code;

    /**
     * 传输类型：sse / stdio
     */
    private String transportType;

    /**
     * 基础 URL（SSE 类型使用）
     */
    private String baseUrl;

    /**
     * 命令（STDIO 类型使用）
     */
    private String command;

    /**
     * 参数 JSON（STDIO 类型使用）
     */
    @TableField(typeHandler = JsonbTypeHandler.class)
    private Map<String, Object> argsJson;

    /**
     * 环境变量 JSON（STDIO 类型使用）
     */
    @TableField(typeHandler = JsonbTypeHandler.class)
    private Map<String, Object> envJson;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 状态：active / error / offline
     */
    private String status;

    /**
     * 最后一次心跳时间
     */
    private LocalDateTime lastHeartbeatAt;
}
