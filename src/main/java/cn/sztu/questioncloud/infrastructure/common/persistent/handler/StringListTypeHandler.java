package cn.sztu.questioncloud.infrastructure.common.persistent.handler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 用于 PostgreSQL 的 TEXT[] 和 Java 的 List<String> 之间的类型转换
 */
@MappedJdbcTypes(JdbcType.ARRAY)
@MappedTypes(List.class)
public class StringListTypeHandler extends BaseTypeHandler<List<String>> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<String> parameter, JdbcType jdbcType) throws SQLException {
        Array sqlArray = ps.getConnection().createArrayOf("text", parameter.toArray());
        ps.setArray(i, sqlArray);
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return getListFromSqlArray(rs.getArray(columnName));
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return getListFromSqlArray(rs.getArray(columnIndex));
    }

    @Override
    public List<String> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return getListFromSqlArray(cs.getArray(columnIndex));
    }

    private List<String> getListFromSqlArray(Array sqlArray) throws SQLException {
        if (sqlArray == null) {
            return Collections.emptyList();
        }
        try {
            return Arrays.asList((String[]) sqlArray.getArray());
        } finally {
            sqlArray.free();
        }
    }
}
