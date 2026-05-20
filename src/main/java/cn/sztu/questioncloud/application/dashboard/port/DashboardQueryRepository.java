package cn.sztu.questioncloud.application.dashboard.port;

import cn.sztu.questioncloud.application.dashboard.dto.DashboardRecentItemDTO;
import cn.sztu.questioncloud.application.dashboard.dto.DashboardStatsDTO;

import java.time.LocalDateTime;
import java.util.List;

public interface DashboardQueryRepository {
    DashboardStatsDTO queryStats(Long userId, LocalDateTime todayStart);

    List<DashboardRecentItemDTO> listRecentItems(Long userId, int limit);
}
