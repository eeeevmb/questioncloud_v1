package cn.sztu.questioncloud.infrastructure.common.persistent.handler;

import cn.sztu.questioncloud.infrastructure.common.persistent.entity.dto.AssetSnapshot;
import cn.sztu.questioncloud.infrastructure.common.persistent.enums.AssetSection;
import cn.sztu.questioncloud.infrastructure.common.persistent.enums.AssetType;
import org.apache.ibatis.type.JdbcType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssetSnapshotListTypeHandlerTests {

    private final AssetSnapshotListTypeHandler handler = new AssetSnapshotListTypeHandler();

    @Mock
    PreparedStatement preparedStatement;

    @Mock
    ResultSet resultSet;

//    @Test
    @DisplayName("setNonNullParameter 应写入 JSON 字符串")
    void setNonNullParameterWritesJson() throws SQLException {
        List<AssetSnapshot> snapshots = List.of(
                new AssetSnapshot("pro-1", AssetSection.PRO, 1, AssetType.IMAGE,
                        "asset://pro-1", 101L, "uploads/img-101.png")
        );

        handler.setNonNullParameter(preparedStatement, 1, snapshots, JdbcType.VARCHAR);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(preparedStatement).setString(eq(1), captor.capture());
        assertThat(captor.getValue())
                .contains("\"slotId\":\"pro-1\"")
                .contains("\"section\":\"PRO\"");
    }

//    @Test
    @DisplayName("getNullableResult 解析 JSON")
    void getNullableResultParsesJson() throws SQLException {
        when(resultSet.getString("assets")).thenReturn("""
                [{
                  "slotId":"pro-1",
                  "section":"PRO",
                  "ordinal":1,
                  "type":"IMAGE",
                  "latexPlaceholder":"asset://pro-1",
                  "fileId":101,
                  "storagePath":"uploads/img-101.png"
                }]
                """);

        List<AssetSnapshot> result = handler.getNullableResult(resultSet, "assets");

        assertThat(result)
                .hasSize(1);
        AssetSnapshot snapshot = result.get(0);
        assertThat(snapshot.slotId()).isEqualTo("pro-1");
        assertThat(snapshot.section()).isEqualTo(AssetSection.PRO);
        assertThat(snapshot.fileId()).isEqualTo(101L);
    }

//    @Test
    @DisplayName("getNullableResult 空串时返回空列表")
    void getNullableResultHandlesBlank() throws SQLException {
        when(resultSet.getString("assets")).thenReturn("");

        assertThat(handler.getNullableResult(resultSet, "assets")).isEmpty();
    }
//
//    @Test
    @DisplayName("getNullableResult 非法 JSON 抛出 SQLException")
    void getNullableResultThrowsOnInvalidJson() throws SQLException {
        when(resultSet.getString("assets")).thenReturn("not-json");

        assertThatThrownBy(() -> handler.getNullableResult(resultSet, "assets"))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("deserialize");
    }
}
