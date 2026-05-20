package cn.sztu.questioncloud.infrastructure.common.persistent.mapper.dashboard;

import cn.sztu.questioncloud.application.dashboard.dto.DashboardRecentItemDTO;
import cn.sztu.questioncloud.application.dashboard.dto.DashboardStatsDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface DashboardQueryMapper {
    @Select("""
            SELECT
              (SELECT COUNT(*) FROM question WHERE owner_id = #{userId}) AS questionTotal,
              (SELECT COUNT(*) FROM question WHERE owner_id = #{userId} AND created_at >= #{todayStart}) AS questionTodayDelta,
              (SELECT COUNT(*) FROM question_collection WHERE owner_id = #{userId}) AS collectionTotal,
              (SELECT COUNT(*) FROM question_collection WHERE owner_id = #{userId} AND created_at >= #{todayStart}) AS collectionTodayDelta,
              (SELECT COUNT(*) FROM paper WHERE owner_id = #{userId}) AS paperTotal,
              (SELECT COUNT(*) FROM paper WHERE owner_id = #{userId} AND created_at >= #{todayStart}) AS paperTodayDelta,
              (SELECT COUNT(*) FROM import_session WHERE user_id = #{userId}) AS importTotal,
              (SELECT COUNT(*) FROM import_session WHERE user_id = #{userId} AND created_at >= #{todayStart}) AS importTodayDelta
            """)
    DashboardStatsDTO queryStats(@Param("userId") Long userId,
                                 @Param("todayStart") LocalDateTime todayStart);

    @Select("""
            SELECT id, name, type, updatedAt
            FROM (
              SELECT CAST(id AS CHAR) AS id,
                     name AS name,
                     'COLLECTION' AS type,
                     updated_at AS updatedAt
              FROM question_collection
              WHERE owner_id = #{userId}

              UNION ALL

              SELECT CAST(id AS CHAR) AS id,
                     title AS name,
                     'PAPER' AS type,
                     updated_at AS updatedAt
              FROM paper
              WHERE owner_id = #{userId}

              UNION ALL

              SELECT CAST(id AS CHAR) AS id,
                     CONCAT('导入任务 ', id) AS name,
                     'IMPORT' AS type,
                     updated_at AS updatedAt
              FROM import_session
              WHERE user_id = #{userId}

              UNION ALL

              SELECT CAST(q.id AS CHAR) AS id,
                     COALESCE(NULLIF(qv.title, ''), LEFT(REPLACE(REPLACE(qv.stem, CHAR(10), ''), CHAR(13), ''), 20), '未命名题目') AS name,
                     'QUESTION' AS type,
                     q.updated_at AS updatedAt
              FROM question q
              LEFT JOIN question_version qv ON q.current_version_id = qv.id
              WHERE q.owner_id = #{userId}
            ) recent
            ORDER BY updatedAt DESC
            LIMIT #{limit}
            """)
    List<DashboardRecentItemDTO> listRecentItems(@Param("userId") Long userId,
                                                 @Param("limit") int limit);
}
