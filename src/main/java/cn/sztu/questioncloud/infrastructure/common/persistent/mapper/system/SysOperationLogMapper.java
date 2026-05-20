package cn.sztu.questioncloud.infrastructure.common.persistent.mapper.system;

import cn.sztu.questioncloud.infrastructure.common.persistent.entity.system.SysOperationLogEntity;
import cn.xbatis.core.mybatis.mapper.MybatisMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysOperationLogMapper extends MybatisMapper<SysOperationLogEntity> {
    @Select("""
            SELECT *
            FROM sys_operation_log
            WHERE user_id = #{userId}
              AND result = 'SUCCESS'
            ORDER BY occurred_at DESC
            LIMIT #{limit}
            """)
    List<SysOperationLogEntity> listRecentSuccessActivities(@Param("userId") Long userId,
                                                            @Param("limit") int limit);
}
