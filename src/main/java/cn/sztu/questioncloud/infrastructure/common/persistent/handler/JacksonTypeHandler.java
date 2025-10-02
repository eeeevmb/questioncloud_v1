package cn.sztu.questioncloud.infrastructure.common.persistent.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

// 适用于 MySQL：JSON 列通过字符串读写
@MappedJdbcTypes(JdbcType.VARCHAR) // 驱动一般把 JSON 当 VARCHAR 传输；如果你的驱动映射为 OTHER，可改为 OTHER
public abstract class JacksonTypeHandler<T> extends BaseTypeHandler<T> {
    private static final ObjectMapper M = new ObjectMapper();
    private final Class<T> clazz;

    protected JacksonTypeHandler(Class<T> clazz) { this.clazz = clazz; }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, T parameter, JdbcType jdbcType) throws SQLException {
        try { ps.setString(i, M.writeValueAsString(parameter)); }
        catch (Exception e) { throw new SQLException("Serialize JSON failed", e); }
    }
    @Override public T getNullableResult(ResultSet rs, String columnName) throws SQLException { return parse(rs.getString(columnName)); }
    @Override public T getNullableResult(ResultSet rs, int columnIndex)  throws SQLException { return parse(rs.getString(columnIndex)); }
    @Override public T getNullableResult(CallableStatement cs, int columnIndex) throws SQLException { return parse(cs.getString(columnIndex)); }

    private T parse(String s) throws SQLException {
        if (s == null || s.isBlank()) return null;
        try { return M.readValue(s, clazz); }
        catch (Exception e) { throw new SQLException("Parse JSON failed", e); }
    }
}