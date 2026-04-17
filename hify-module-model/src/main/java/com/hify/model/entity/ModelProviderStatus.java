package com.hify.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 模型提供商运行时健康状态
 *
 * @author hify
 */
@Data
@TableName("model_provider_status")
public class ModelProviderStatus implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 提供商 ID（与 model_provider 1:1）
     */
    @TableId
    private Long providerId;

    /**
     * 健康状态：healthy | degraded | unhealthy | unknown
     */
    private String healthStatus;

    /**
     * 最近检查时间
     */
    private LocalDateTime healthCheckedAt;

    /**
     * 检查延迟（毫秒）
     */
    private Integer healthLatencyMs;

    /**
     * 检查错误信息
     */
    private String healthErrorMsg;

    /**
     * 总请求数
     */
    private Long totalRequests;

    /**
     * 失败请求数
     */
    private Long failedRequests;

    /**
     * 最近错误时间
     */
    private LocalDateTime lastErrorAt;

    /**
     * 最近错误代码
     */
    private String lastErrorCode;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
