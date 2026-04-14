package com.hify.common.web.security;

/**
 * 用户上下文（ThreadLocal）
 *
 * @author hify
 */
public class UserContext {

    private static final ThreadLocal<CurrentUser> CURRENT_USER = new ThreadLocal<>();

    /**
     * 设置当前用户
     */
    public static void set(CurrentUser user) {
        CURRENT_USER.set(user);
    }

    /**
     * 获取当前用户
     */
    public static CurrentUser get() {
        return CURRENT_USER.get();
    }

    /**
     * 获取当前用户 ID
     */
    public static Long getUserId() {
        CurrentUser user = get();
        return user != null ? user.getUserId() : null;
    }

    /**
     * 清除当前用户
     */
    public static void clear() {
        CURRENT_USER.remove();
    }
}
