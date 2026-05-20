package cn.sztu.questioncloud.web.rest.v1.dashboard.vo;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class DashboardAnnouncementVO {
    private String title;
    private LocalDate date;
}
