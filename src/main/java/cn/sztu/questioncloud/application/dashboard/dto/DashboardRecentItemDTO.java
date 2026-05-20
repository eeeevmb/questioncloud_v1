package cn.sztu.questioncloud.application.dashboard.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DashboardRecentItemDTO {
    private String id;
    private String name;
    private String type;
    private LocalDateTime updatedAt;
}
