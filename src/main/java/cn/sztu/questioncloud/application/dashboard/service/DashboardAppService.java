package cn.sztu.questioncloud.application.dashboard.service;

import cn.sztu.questioncloud.web.rest.v1.dashboard.vo.DashboardOverviewVO;

public interface DashboardAppService {
    DashboardOverviewVO getOverview();
}
