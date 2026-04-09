package com.hify.common.core.constants;

/**
 * 通用常量
 *
 * @author hify
 */
public final class CommonConstants {

    private CommonConstants() {
        throw new UnsupportedOperationException("常量类禁止实例化");
    }

    /**
     * 编码格式
     */
    public static final String UTF_8 = "UTF-8";

    /**
     * 时间格式
     */
    public static final String DATE_FORMAT = "yyyy-MM-dd";
    public static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final String TIME_FORMAT = "HH:mm:ss";

    /**
     * 分隔符
     */
    public static final String COMMA = ",";
    public static final String SEMICOLON = ";";
    public static final String COLON = ":";
    public static final String SLASH = "/";
    public static final String DASH = "-";
    public static final String UNDERSCORE = "_";
    public static final String DOT = ".";
    public static final String PIPE = "|";

    /**
     * 布尔字符串
     */
    public static final String TRUE = "true";
    public static final String FALSE = "false";

    /**
     * 常用数字
     */
    public static final int ZERO = 0;
    public static final int ONE = 1;
    public static final int TEN = 10;
    public static final int HUNDRED = 100;
    public static final int THOUSAND = 1000;

    /**
     * 分页默认值
     */
    public static final int DEFAULT_PAGE_NUM = 1;
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    /**
     * ID 生成相关
     */
    public static final long SNOWFLAKE_WORKER_ID = 1L;
    public static final long SNOWFLAKE_DATACENTER_ID = 1L;

}
