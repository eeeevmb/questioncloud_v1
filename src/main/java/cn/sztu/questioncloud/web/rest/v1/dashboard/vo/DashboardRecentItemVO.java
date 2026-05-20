package cn.sztu.questioncloud.web.rest.v1.dashboard.vo;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class DashboardRecentItemVO {
    private String id;
    private String name;
    private String type;
    private LocalDateTime updatedAt;
    private String targetPath;
}
