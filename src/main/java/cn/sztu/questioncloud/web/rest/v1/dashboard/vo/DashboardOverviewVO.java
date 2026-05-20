package cn.sztu.questioncloud.web.rest.v1.dashboard.vo;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class DashboardOverviewVO {
    private DashboardWelcomeVO welcome;
    private DashboardStatsVO stats;
    private List<DashboardRecentItemVO> recentItems;
    private List<DashboardAnnouncementVO> announcements;
    private List<DashboardActivityVO> activities;
}
