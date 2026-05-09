package com.hify.mcp.api.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * MCP Server DTO
 */
@Data
public class McpServerDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String code;
    private String transportType;
    private String baseUrl;
    private String command;
    private Map<String, Object> argsJson;
    private Map<String, Object> envJson;
    private Boolean enabled;
    private String status;
    private LocalDateTime lastHeartbeatAt;
}
