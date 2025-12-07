package cn.sztu.questioncloud.application.question.port;

import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.QuestionStat;

import java.util.List;

public interface QuestionStatRepository {
    /**
     * 保存单个统计数据
     * @param stat 统计数据
     */
    void save(QuestionStat stat);

    /**
     * 批量保存统计数据
     * @param stats 统计数据列表
     */
    void batchSave(List<QuestionStat> stats);
}
