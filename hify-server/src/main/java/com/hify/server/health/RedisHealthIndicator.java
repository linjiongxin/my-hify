package com.hify.server.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis 健康检查
 *
 * @author hify
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisHealthIndicator implements HealthIndicator {

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public Health health() {
        try {
            String pong = stringRedisTemplate.getConnectionFactory()
                    .getConnection()
                    .ping();
            if ("PONG".equalsIgnoreCase(pong)) {
                return Health.up()
                        .withDetail("redis", "available")
                        .build();
            }
            return Health.down()
                    .withDetail("redis", "unavailable")
                    .withDetail("response", pong)
                    .build();
        } catch (Exception e) {
            log.warn("Redis 健康检查失败", e);
            return Health.down()
                    .withDetail("redis", "error")
                    .withDetail("message", e.getMessage())
                    .build();
        }
    }
}
