package cn.sztu.questioncloud.application.paper.port;

public interface PaperPresenceCheckerRepository {
    /**
     * 校验用户是否存在同名试卷
     *
     * @param title 试卷title
     * @param userId 用户ID
     * @return 是否存在
     */
    boolean existsByTitle(String title, Long userId);
}
