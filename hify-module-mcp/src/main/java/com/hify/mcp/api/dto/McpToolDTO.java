package com.hify.mcp.api.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * MCP Tool DTO
 */
@Data
public class McpToolDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long serverId;
    private String name;
    private String description;
    private Map<String, Object> schemaJson;
    private Boolean enabled;
}
