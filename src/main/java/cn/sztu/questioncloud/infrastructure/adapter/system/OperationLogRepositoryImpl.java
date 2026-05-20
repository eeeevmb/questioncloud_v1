package cn.sztu.questioncloud.infrastructure.adapter.system;

import cn.sztu.questioncloud.application.system.port.OperationLogRepository;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.system.SysOperationLogEntity;
import cn.sztu.questioncloud.infrastructure.common.persistent.mapper.system.SysOperationLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class OperationLogRepositoryImpl implements OperationLogRepository {
    private final SysOperationLogMapper sysOperationLogMapper;

    @Override
    public void save(SysOperationLogEntity entity) {
        sysOperationLogMapper.save(entity);
    }

    @Override
    public List<SysOperationLogEntity> listRecentSuccessActivities(Long userId, int limit) {
        return sysOperationLogMapper.listRecentSuccessActivities(userId, limit);
    }
}
