package com.hify.server.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * 数据库健康检查
 *
 * @author hify
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseHealthIndicator implements HealthIndicator {

    private final DataSource dataSource;

    @Override
    public Health health() {
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(3)) {
                return Health.up()
                        .withDetail("database", "available")
                        .build();
            }
            return Health.down()
                    .withDetail("database", "unavailable")
                    .build();
        } catch (Exception e) {
            log.warn("数据库健康检查失败", e);
            return Health.down()
                    .withDetail("database", "error")
                    .withDetail("message", e.getMessage())
                    .build();
        }
    }
}
