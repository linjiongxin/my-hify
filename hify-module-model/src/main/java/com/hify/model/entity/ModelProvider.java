package com.hify.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.hify.common.web.entity.base.BaseEntity;
import com.hify.common.web.handler.JsonbTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/**
 * 模型提供商
 *
 * @author hify
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("model_provider")
public class ModelProvider extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 提供商名称
     */
    private String name;

    /**
     * 提供商代码（如 openai / deepseek / qwen）
     */
    private String code;

    /**
     * 协议类型（决定后端用哪个 LlmProvider 实现处理）
     */
    private String protocolType;

    /**
     * API 基础地址
     */
    private String apiBaseUrl;

    /**
     * 鉴权类型：BEARER | API_KEY | NONE | CUSTOM
     */
    private String authType;

    /**
     * 主 API Key
     */
    private String apiKey;

    /**
     * 结构化额外鉴权参数
     */
    @TableField(typeHandler = JsonbTypeHandler.class)
    private Map<String, Object> authConfig;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 排序
     */
    private Integer sortOrder;
}
