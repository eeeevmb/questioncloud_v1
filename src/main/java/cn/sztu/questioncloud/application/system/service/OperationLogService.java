package cn.sztu.questioncloud.application.system.service;

import cn.sztu.questioncloud.application.system.dto.OperationLogActivityDTO;
import cn.sztu.questioncloud.application.system.dto.OperationLogCreateCommand;

import java.util.List;

public interface OperationLogService {
    void record(OperationLogCreateCommand command);

    List<OperationLogActivityDTO> listRecentActivities(Long userId, int limit);
}
