package cn.sztu.questioncloud.web.rest.v1.dashboard.vo;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class DashboardActivityVO {
    private String type;
    private String username;
    private String content;
    private LocalDateTime occurredAt;
    private String timeText;
}
