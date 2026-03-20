package cn.sztu.questioncloud.application.question.port;

import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.QuestionStat;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface QuestionStatRepository {
    /**
     * 根据题目ID查询统计数据
     *
     * @param questionId 题目ID
     * @return 统计数据
     */
    QuestionStat getByQuestionId(Long questionId);

    /**
     * 根据题目ID列表批量查询统计数据
     *
     * @param questionIds 题目ID列表
     * @return            统计数据表
     */
    Map<Long, QuestionStat> getByQuestionIds(Collection<Long> questionIds);

    /**
     * 根据实体更新统计数据
     *
     * @param questionStat 统计数据
     */
    void update(QuestionStat questionStat);

    /**
     * 根据题目ID删除统计数据
     *
     * @param questionId 题目ID
     */
    void deleteByQuestionId(Long questionId);

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
