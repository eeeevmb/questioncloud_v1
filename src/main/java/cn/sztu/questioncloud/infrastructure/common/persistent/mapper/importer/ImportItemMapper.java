package cn.sztu.questioncloud.infrastructure.common.persistent.mapper.importer;

import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.ImportItemEntity;
import cn.xbatis.core.mybatis.mapper.MybatisMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ImportItemMapper extends MybatisMapper<ImportItemEntity> {
    @Select({"<script>",
            "SELECT * FROM import_item",
            "WHERE import_id = #{importId}",
            "<if test='status != null'> AND status = #{status}</if>",
            "ORDER BY index_no",
            "LIMIT #{offset}, #{limit}",
            "</script>"})
    List<ImportItemEntity> selectPage(@Param("importId") Long importId,
                                      @Param("status") Integer status,
                                      @Param("offset") int offset,
                                      @Param("limit") int limit);

    @Select({"<script>",
            "SELECT COUNT(*) FROM import_item",
            "WHERE import_id = #{importId}",
            "<if test='status != null'> AND status = #{status}</if>",
            "</script>"})
    long countByImportId(@Param("importId") Long importId,
                         @Param("status") Integer status);
}
