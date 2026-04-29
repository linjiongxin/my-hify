package com.hify.common.web.handler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.UUID;

/**
 * PostgreSQL UUID 类型处理器
 * <p>
 * 映射 PostgreSQL UUID 列与 Java UUID
 * </p>
 * <p>
 * 使用方式（任选其一）：
 * <ul>
 *   <li>全局注册后自动生效（见 {@link com.hify.common.web.config.MybatisPlusConfig}）</li>
 *   <li>字段级别注解：{@code @TableField(typeHandler = UUIDTypeHandler.class)}</li>
 * </ul>
 * </p>
 *
 * <pre>
 * CREATE TABLE example (
 *     id UUID PRIMARY KEY,
 *     user_id UUID NOT NULL
 * );
 * </pre>
 */
public class UUIDTypeHandler extends BaseTypeHandler<UUID> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, UUID parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setObject(i, parameter, Types.OTHER);
    }

    @Override
    public UUID getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return rs.getObject(columnName, UUID.class);
    }

    @Override
    public UUID getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return rs.getObject(columnIndex, UUID.class);
    }

    @Override
    public UUID getNullableResult(java.sql.CallableStatement cs, int columnIndex) throws SQLException {
        return cs.getObject(columnIndex, UUID.class);
    }
}
