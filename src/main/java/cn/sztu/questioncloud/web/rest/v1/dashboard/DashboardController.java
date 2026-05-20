package cn.sztu.questioncloud.web.rest.v1.dashboard;

import cn.sztu.questioncloud.application.dashboard.service.DashboardAppService;
import cn.sztu.questioncloud.common.model.vo.ResultVO;
import cn.sztu.questioncloud.web.rest.v1.dashboard.vo.DashboardOverviewVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/dashboard")
public class DashboardController {
    private final DashboardAppService dashboardAppService;

    @GetMapping("/overview")
    public ResultVO<DashboardOverviewVO> getOverview() {
        return ResultVO.success(dashboardAppService.getOverview());
    }
}
