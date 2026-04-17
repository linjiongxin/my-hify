package com.hify.server.health;

import com.hify.model.api.ModelProviderApi;
import com.hify.model.api.dto.ModelProviderDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * LLM Provider 健康检查
 *
 * @author hify
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LlmProviderHealthIndicator implements HealthIndicator {

    private final ModelProviderApi modelProviderApi;

    @Override
    public Health health() {
        try {
            List<ModelProviderDTO> providers = modelProviderApi.listEnabledProviders();

            if (providers.isEmpty()) {
                return Health.down()
                        .withDetail("llmProvider", "no enabled providers")
                        .build();
            }

            return Health.up()
                    .withDetail("llmProvider", "available")
                    .withDetail("enabledCount", providers.size())
                    .build();
        } catch (Exception e) {
            log.warn("LLM Provider 健康检查失败", e);
            return Health.down()
                    .withDetail("llmProvider", "error")
                    .withDetail("message", e.getMessage())
                    .build();
        }
    }
}
