package com.hify.common.core.exception;

import com.hify.common.core.enums.ResultCode;
import lombok.Getter;

/**
 * 业务异常
 * <p>继承 RuntimeException，业务异常不强制捕获</p>
 *
 * @author hify
 */
@Getter
public class BizException extends RuntimeException {

    private final Integer code;
    private final String message;

    public BizException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
        this.message = resultCode.getMessage();
    }

    public BizException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
        this.message = message;
    }

    public BizException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    public BizException(String message) {
        super(message);
        this.code = ResultCode.SYSTEM_ERROR.getCode();
        this.message = message;
    }

    public BizException(String message, Throwable cause) {
        super(message, cause);
        this.code = ResultCode.SYSTEM_ERROR.getCode();
        this.message = message;
    }

}
