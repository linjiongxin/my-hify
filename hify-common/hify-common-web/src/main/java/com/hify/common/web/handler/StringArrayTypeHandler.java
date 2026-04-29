package com.hify.common.web.handler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeException;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * PostgreSQL ARRAY 类型处理器
 * <p>
 * 映射 PostgreSQL TEXT[] / INT[] 等数组列与 Java String[] / Integer[] / Long[]
 * </p>
 * <p>
 * 使用方式（字段级注解）：
 * <pre>
 * // String[] → TEXT[]
 * &#64;TableField(typeHandler = StringArrayTypeHandler.class)
 * private String[] tags;
 *
 * // Integer[] → INT[]
 * &#64;TableField(typeHandler = StringArrayTypeHandler.class)
 * private Integer[] scores;
 * </pre>
 * </p>
 *
 * <pre>
 * CREATE TABLE example (
 *     id BIGSERIAL PRIMARY KEY,
 *     tags TEXT[],          -- String[]
 *     scores INT[]          -- Integer[]
 * );
 * </pre>
 */
public class StringArrayTypeHandler extends BaseTypeHandler<Object> {

    private Class<?> elementType = String.class;

    public StringArrayTypeHandler() {
    }

    /**
     * 指定元素类型构造
     */
    public StringArrayTypeHandler(Class<?> elementType) {
        this.elementType = elementType;
    }

    public void setElementType(Class<?> elementType) {
        this.elementType = elementType;
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Object parameter, JdbcType jdbcType)
            throws SQLException {
        if (parameter == null) {
            ps.setNull(i, Types.OTHER);
            return;
        }
        String pgArray = toPgArrayString(parameter);
        ps.setObject(i, pgArray, Types.OTHER);
    }

    @Override
    public Object getNullableResult(ResultSet rs, String columnName) throws SQLException {
        Object raw = rs.getObject(columnName);
        return parsePgArray(raw);
    }

    @Override
    public Object getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        Object raw = rs.getObject(columnIndex);
        return parsePgArray(raw);
    }

    @Override
    public Object getNullableResult(java.sql.CallableStatement cs, int columnIndex) throws SQLException {
        Object raw = cs.getObject(columnIndex);
        return parsePgArray(raw);
    }

    // ---------- private 转换方法 ----------

    private String toPgArrayString(Object arr) {
        if (arr instanceof String[] stringArray) {
            return toPgArray(stringArray);
        } else if (arr instanceof Integer[] intArray) {
            return toPgArrayInt(intArray);
        } else if (arr instanceof Long[] longArray) {
            return toPgArrayLong(longArray);
        } else if (arr instanceof List<?> list) {
            return toPgArrayList(list);
        }
        throw new TypeException(
            "StringArrayTypeHandler 不支持类型: " + arr.getClass().getName());
    }

    private String toPgArray(String[] arr) {
        if (arr.length == 0) return "{}";
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < arr.length; i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(escape(arr[i])).append("\"");
        }
        sb.append("}");
        return sb.toString();
    }

    private String toPgArrayInt(Integer[] arr) {
        if (arr.length == 0) return "{}";
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < arr.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(arr[i] == null ? "NULL" : arr[i]);
        }
        sb.append("}");
        return sb.toString();
    }

    private String toPgArrayLong(Long[] arr) {
        if (arr.length == 0) return "{}";
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < arr.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(arr[i] == null ? "NULL" : arr[i]);
        }
        sb.append("}");
        return sb.toString();
    }

    private String toPgArrayList(List<?> list) {
        if (list.isEmpty()) return "{}";
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            Object v = list.get(i);
            if (v == null) {
                sb.append("NULL");
            } else if (v instanceof Number num) {
                sb.append(num);
            } else {
                sb.append("\"").append(escape(String.valueOf(v))).append("\"");
            }
        }
        sb.append("}");
        return sb.toString();
    }

    private Object parsePgArray(Object raw) {
        if (raw == null) return null;

        String str = raw.toString();
        str = str.trim();
        if (str.startsWith("{") && str.endsWith("}")) {
            str = str.substring(1, str.length() - 1);
        }
        if (str.isEmpty()) {
            return java.lang.reflect.Array.newInstance(elementType, 0);
        }

        List<String> tokens = new ArrayList<>();
        boolean inQuote = false;
        StringBuilder token = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '"') {
                inQuote = !inQuote;
            } else if (c == ',' && !inQuote) {
                tokens.add(token.toString().trim());
                token = new StringBuilder();
            } else {
                token.append(c);
            }
        }
        if (token.length() > 0) {
            tokens.add(token.toString().trim());
        }

        if (elementType == String.class) {
            String[] result = tokens.toArray(new String[0]);
            for (int i = 0; i < result.length; i++) {
                result[i] = unescape(result[i]);
            }
            return result;
        } else if (elementType == Integer.class) {
            Integer[] result = new Integer[tokens.size()];
            for (int i = 0; i < tokens.size(); i++) {
                String t = tokens.get(i);
                result[i] = t.isEmpty() || "NULL".equalsIgnoreCase(t) ? null : Integer.parseInt(t);
            }
            return result;
        } else if (elementType == Long.class) {
            Long[] result = new Long[tokens.size()];
            for (int i = 0; i < tokens.size(); i++) {
                String t = tokens.get(i);
                result[i] = t.isEmpty() || "NULL".equalsIgnoreCase(t) ? null : Long.parseLong(t);
            }
            return result;
        }

        // fallback to String[]
        String[] result = tokens.toArray(new String[0]);
        for (int i = 0; i < result.length; i++) {
            result[i] = unescape(result[i]);
        }
        return result;
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String unescape(String s) {
        if (s != null && s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
            s = s.substring(1, s.length() - 1);
        }
        if (s != null) {
            s = s.replace("\\\"", "\"").replace("\\\\", "\\");
        }
        return s;
    }
}
