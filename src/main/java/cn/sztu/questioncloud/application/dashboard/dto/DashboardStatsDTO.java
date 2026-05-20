package cn.sztu.questioncloud.application.dashboard.dto;

import lombok.Data;

@Data
public class DashboardStatsDTO {
    private Long questionTotal;
    private Long questionTodayDelta;
    private Long collectionTotal;
    private Long collectionTodayDelta;
    private Long paperTotal;
    private Long paperTodayDelta;
    private Long importTotal;
    private Long importTodayDelta;
}
