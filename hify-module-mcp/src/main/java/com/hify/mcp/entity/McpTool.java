package com.hify.mcp.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.hify.common.web.entity.base.BaseEntity;
import com.hify.common.web.handler.JsonbTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/**
 * MCP Tool 实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mcp_tool")
public class McpTool extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * MCP Server ID
     */
    private Long serverId;

    /**
     * 工具名称
     */
    private String name;

    /**
     * 工具描述
     */
    private String description;

    /**
     * 参数 JSON Schema
     */
    @TableField(typeHandler = JsonbTypeHandler.class)
    private Map<String, Object> schemaJson;

    /**
     * 是否启用
     */
    private Boolean enabled;
}
