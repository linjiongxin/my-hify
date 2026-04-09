package com.hify.common.web.api;

import com.hify.common.web.entity.Result;

/**
 * 通用 API 接口
 *
 * @author hify
 */
public interface BaseApi {

    /**
     * 健康检查
     */
    default Result<String> health() {
        return Result.success("ok");
    }

    /**
     * 版本信息
     */
    default Result<String> version() {
        return Result.success("1.0.0");
    }

}
