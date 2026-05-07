package com.hify.workflow.engine.util;

import com.hify.workflow.engine.context.ExecutionContext;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 占位符替换工具
 * <p>统一处理 ${variable} 格式的变量替换</p>
 */
public final class PlaceholderUtils {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}]+)}");

    private PlaceholderUtils() {
        // utility class
    }

    /**
     * 替换字符串中的 ${variable} 占位符
     *
     * @param template 模板字符串
     * @param context  执行上下文
     * @return 替换后的字符串，template 为 null 时返回 null
     */
    public static String replace(String template, ExecutionContext context) {
        if (template == null) {
            return null;
        }

        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String varName = matcher.group(1);
            Object value = context.get(varName);
            matcher.appendReplacement(result, Matcher.quoteReplacement(value != null ? value.toString() : ""));
        }

        matcher.appendTail(result);
        return result.toString();
    }
}
