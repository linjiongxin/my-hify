package com.hify.common.web.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口幂等性注解
 * <p>基于请求头 {@code Idempotency-Key} 实现幂等控制</p>
 *
 * @author hify
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {

    /**
     * Redis Key 前缀
     */
    String keyPrefix() default "idempotent:";

    /**
     * 过期时间（秒）
     */
    long expireSeconds() default 60;

    /**
     * 是否要求必须携带 Idempotency-Key
     */
    boolean required() default true;

    /**
     * 重复请求时的提示信息
     */
    String message() default "重复请求，请稍后再试";
}
