package com.hify.agent.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class AgentToolBatchRequest {
    private List<ToolItem> tools;

    @Data
    public static class ToolItem {
        private String toolName;
        private String toolType;
        private String toolImpl;
        private Map<String, Object> configJson;
        private Boolean enabled = true;
        private Integer sortOrder = 0;
    }
}
