package cn.sztu.questioncloud.application.dashboard.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.sztu.questioncloud.application.dashboard.dto.DashboardRecentItemDTO;
import cn.sztu.questioncloud.application.dashboard.dto.DashboardStatsDTO;
import cn.sztu.questioncloud.application.dashboard.port.DashboardQueryRepository;
import cn.sztu.questioncloud.application.dashboard.service.DashboardAppService;
import cn.sztu.questioncloud.application.system.service.OperationLogService;
import cn.sztu.questioncloud.application.user.port.UserAccountRepository;
import cn.sztu.questioncloud.common.constant.enums.result.impl.CommonResultCodeEnum;
import cn.sztu.questioncloud.common.exception.ApplicationException;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.user.UserAccountEntity;
import cn.sztu.questioncloud.web.rest.v1.dashboard.vo.DashboardActivityVO;
import cn.sztu.questioncloud.web.rest.v1.dashboard.vo.DashboardOverviewVO;
import cn.sztu.questioncloud.web.rest.v1.dashboard.vo.DashboardRecentItemVO;
import cn.sztu.questioncloud.web.rest.v1.dashboard.vo.DashboardStatsVO;
import cn.sztu.questioncloud.web.rest.v1.dashboard.vo.DashboardWelcomeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardAppServiceImpl implements DashboardAppService {
    private static final int DEFAULT_ITEM_LIMIT = 5;

    private final DashboardQueryRepository dashboardQueryRepository;
    private final OperationLogService operationLogService;
    private final UserAccountRepository userAccountRepository;

    @Override
    public DashboardOverviewVO getOverview() {
        Long userId = StpUtil.getLoginIdAsLong();
        UserAccountEntity user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ApplicationException(CommonResultCodeEnum.NOT_FOUND, "用户不存在"));

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        DashboardStatsDTO stats = dashboardQueryRepository.queryStats(userId, todayStart);

        return DashboardOverviewVO.builder()
                .welcome(DashboardWelcomeVO.builder()
                        .username(user.getUsername())
                        .build())
                .stats(toStatsVO(stats))
                .recentItems(dashboardQueryRepository.listRecentItems(userId, DEFAULT_ITEM_LIMIT)
                        .stream()
                        .map(this::toRecentItemVO)
                        .toList())
                .announcements(List.of())
                .activities(operationLogService.listRecentActivities(userId, DEFAULT_ITEM_LIMIT)
                        .stream()
                        .map(activity -> DashboardActivityVO.builder()
                                .type(activity.getType())
                                .content(activity.getContent())
                                .occurredAt(activity.getOccurredAt())
                                .timeText(activity.getTimeText())
                                .build())
                        .toList())
                .build();
    }

    private DashboardStatsVO toStatsVO(DashboardStatsDTO stats) {
        DashboardStatsDTO safe = stats == null ? new DashboardStatsDTO() : stats;
        return DashboardStatsVO.builder()
                .questionTotal(valueOrZero(safe.getQuestionTotal()))
                .questionYesterdayDelta(valueOrZero(safe.getQuestionTodayDelta()))
                .collectionTotal(valueOrZero(safe.getCollectionTotal()))
                .collectionYesterdayDelta(valueOrZero(safe.getCollectionTodayDelta()))
                .paperTotal(valueOrZero(safe.getPaperTotal()))
                .paperYesterdayDelta(valueOrZero(safe.getPaperTodayDelta()))
                .importTotal(valueOrZero(safe.getImportTotal()))
                .importYesterdayDelta(valueOrZero(safe.getImportTodayDelta()))
                .build();
    }

    private DashboardRecentItemVO toRecentItemVO(DashboardRecentItemDTO dto) {
        String name = dto.getName() == null || dto.getName().isBlank() ? "未命名资源" : dto.getName();
        return DashboardRecentItemVO.builder()
                .id(dto.getId())
                .name(name)
                .type(dto.getType())
                .updatedAt(dto.getUpdatedAt())
                .targetPath(buildTargetPath(dto.getType(), dto.getId(), name))
                .build();
    }

    private String buildTargetPath(String type, String id, String name) {
        String encodedName = URLEncoder.encode(name, StandardCharsets.UTF_8);
        return switch (type) {
            case "COLLECTION" -> "/collections/%s/questions?name=%s".formatted(id, encodedName);
            case "PAPER" -> "/papers/%s".formatted(id);
            case "IMPORT" -> "/imports/%s".formatted(id);
            case "QUESTION" -> "/questions/%s".formatted(id);
            default -> "/";
        };
    }

    private long valueOrZero(Long value) {
        return value == null ? 0L : value;
    }
}
