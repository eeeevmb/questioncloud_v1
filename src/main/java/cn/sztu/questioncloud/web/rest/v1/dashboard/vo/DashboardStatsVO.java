package cn.sztu.questioncloud.web.rest.v1.dashboard.vo;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DashboardStatsVO {
    private long questionTotal;
    private long questionYesterdayDelta;
    private long collectionTotal;
    private long collectionYesterdayDelta;
    private long paperTotal;
    private long paperYesterdayDelta;
    private long importTotal;
    private long importYesterdayDelta;
}
