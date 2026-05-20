package cn.sztu.questioncloud.application.system.port;

import cn.sztu.questioncloud.infrastructure.common.persistent.entity.system.SysOperationLogEntity;

import java.util.List;

public interface OperationLogRepository {
    void save(SysOperationLogEntity entity);

    List<SysOperationLogEntity> listRecentSuccessActivities(Long userId, int limit);
}
