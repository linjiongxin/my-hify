package com.hify.server.health;

import com.hify.model.entity.ModelProvider;
import com.hify.model.service.ModelProviderService;
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

    private final ModelProviderService modelProviderService;

    @Override
    public Health health() {
        try {
            List<ModelProvider> providers = modelProviderService.lambdaQuery()
                    .eq(ModelProvider::getEnabled, true)
                    .list();

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
