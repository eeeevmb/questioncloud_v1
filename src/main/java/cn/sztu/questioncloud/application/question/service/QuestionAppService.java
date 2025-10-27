package cn.sztu.questioncloud.application.question.service;

/**
 * 题目模块应用服务
 *
 * @author eeeevmb
 */
public interface QuestionAppService {

    // ===== 写入操作 =====
    /**
     * 创建默认题集
     * 用户注册后调用一次
     *
     * @param userId 用户id
     */
    void createDefaultCollection(Long userId);
}
