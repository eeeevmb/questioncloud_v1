package cn.sztu.questioncloud.infrastructure.common.persistent.handler;

import cn.sztu.questioncloud.application.importer.dto.ImportErrorReport;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

@MappedJdbcTypes(JdbcType.OTHER)
public class ImportErrorListJsonTypeHandler extends BaseTypeHandler<List<ImportErrorReport>> {
    private static final ObjectMapper M = new ObjectMapper();
    private static final TypeReference<List<ImportErrorReport>> TYPE = new TypeReference<>() {};

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<ImportErrorReport> parameter, JdbcType jdbcType) throws SQLException {
        try {
            ps.setString(i, M.writeValueAsString(parameter));
        } catch (Exception e) {
            throw new SQLException("Serialize import errors failed", e);
        }
    }

    @Override
    public List<ImportErrorReport> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parse(rs.getString(columnName));
    }

    @Override
    public List<ImportErrorReport> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parse(rs.getString(columnIndex));
    }

    @Override
    public List<ImportErrorReport> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parse(cs.getString(columnIndex));
    }

    private List<ImportErrorReport> parse(String raw) throws SQLException {
        if (raw == null || raw.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return M.readValue(raw, TYPE);
        } catch (Exception e) {
            throw new SQLException("Parse import errors failed", e);
        }
    }
}
