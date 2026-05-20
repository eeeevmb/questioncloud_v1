package cn.sztu.questioncloud.infrastructure.adapter.dashboard;

import cn.sztu.questioncloud.application.dashboard.dto.DashboardRecentItemDTO;
import cn.sztu.questioncloud.application.dashboard.dto.DashboardStatsDTO;
import cn.sztu.questioncloud.application.dashboard.port.DashboardQueryRepository;
import cn.sztu.questioncloud.infrastructure.common.persistent.mapper.dashboard.DashboardQueryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class DashboardQueryRepositoryImpl implements DashboardQueryRepository {
    private final DashboardQueryMapper dashboardQueryMapper;

    @Override
    public DashboardStatsDTO queryStats(Long userId, LocalDateTime todayStart) {
        return dashboardQueryMapper.queryStats(userId, todayStart);
    }

    @Override
    public List<DashboardRecentItemDTO> listRecentItems(Long userId, int limit) {
        return dashboardQueryMapper.listRecentItems(userId, limit);
    }
}
