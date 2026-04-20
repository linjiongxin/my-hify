package com.hify.agent.api.dto;

import lombok.Data;
import java.io.Serializable;
import java.util.Map;

@Data
public class AgentToolDTO implements Serializable {
    private Long id;
    private String toolName;
    private String toolType;
    private String toolImpl;
    private Map<String, Object> configJson;
    private Boolean enabled;
    private Integer sortOrder;
}
