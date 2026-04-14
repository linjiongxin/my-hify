package com.hify.common.web.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * JWT 配置属性
 *
 * @author hify
 */
@Data
@Component
@ConfigurationProperties(prefix = "hify.jwt")
public class JwtProperties {

    /**
     * 密钥（Base64 编码，建议长度 >= 256 bit）
     */
    private String secret = "hify-default-secret-key-please-change-in-production-environment-12345";

    /**
     * Token 有效期
     */
    private Duration expiration = Duration.ofHours(24);

    /**
     * Token 请求头名称
     */
    private String header = "Authorization";

    /**
     * Token 前缀
     */
    private String prefix = "Bearer ";
}
