package com.hify.common.core.enums;

import lombok.Getter;

/**
 * 统一响应码枚举
 *
 * @author hify
 */
@Getter
public enum ResultCode {

    SUCCESS(200, "成功"),
    PARAM_ERROR(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),
    SYSTEM_ERROR(500, "系统内部错误"),

    // 业务错误码 1000-1999
    LLM_API_ERROR(1000, "模型调用失败"),
    LLM_TIMEOUT(1001, "模型响应超时"),
    LLM_RATE_LIMIT(1002, "模型限流中"),

    // 数据错误码 2000-2999
    DATA_NOT_FOUND(2000, "数据不存在"),
    DATA_EXISTS(2001, "数据已存在"),
    DATA_DELETED(2002, "数据已删除");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

}
