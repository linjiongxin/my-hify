package com.hify.agent.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class AgentUpdateRequest {
    private String name;
    private String description;
    private String modelId;
    private String systemPrompt;
    private BigDecimal temperature;
    private Integer maxTokens;
    private BigDecimal topP;
    private String welcomeMessage;
    private Boolean enabled;
}
