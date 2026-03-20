package cn.sztu.questioncloud.infrastructure.adapter.question;

import cn.sztu.questioncloud.application.question.port.QuestionStatRepository;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.QuestionStat;
import cn.sztu.questioncloud.infrastructure.common.persistent.mapper.question.QuestionStatMapper;
import cn.xbatis.core.mybatis.MybatisBatchUtil;
import cn.xbatis.core.sql.executor.chain.DeleteChain;
import cn.xbatis.core.sql.executor.chain.QueryChain;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Repository
public class QuestionStatRepositoryImpl implements QuestionStatRepository {
    private final QuestionStatMapper questionStatMapper;
    private final SqlSessionFactory sqlSessionFactory;

    public QuestionStatRepositoryImpl(QuestionStatMapper questionStatMapper, SqlSessionFactory sqlSessionFactory) {
        this.questionStatMapper = questionStatMapper;
        this.sqlSessionFactory = sqlSessionFactory;
    }

    /**
     * 根据题目ID查询统计数据
     *
     * @param questionId 题目ID
     * @return 统计数据
     */
    @Override
    public QuestionStat getByQuestionId(Long questionId) {
        return QueryChain.of(questionStatMapper)
                .eq(QuestionStat::getQuestionId, questionId)
                .limit(1)
                .get();
    }

    /**
     * 根据题目ID列表批量查询统计数据
     *
     * @param questionIds 题目ID列表
     * @return 统计数据表
     */
    @Override
    public Map<Long, QuestionStat> getByQuestionIds(Collection<Long> questionIds) {
        return QueryChain.of(questionStatMapper)
                .in(QuestionStat::getQuestionId, questionIds)
                .mapWithKey(QuestionStat::getQuestionId);
    }

    /**
     * 根据实体更新统计数据
     *
     * @param questionStat 统计数据
     */
    @Override
    public void update(QuestionStat questionStat) {
        questionStatMapper.update(questionStat);
    }

    /**
     * 根据题目ID删除统计数据
     *
     * @param questionId 题目ID
     */
    @Override
    public void deleteByQuestionId(Long questionId) {
        DeleteChain.of(questionStatMapper)
                .eq(QuestionStat::getQuestionId, questionId)
                .execute();
    }

    /**
     * 保存单个统计数据
     *
     * @param stat 统计数据
     */
    @Override
    public void save(QuestionStat stat) {
        questionStatMapper.save(stat);
    }

    /**
     * 批量保存统计数据
     *
     * @param stats 统计数据列表
     */
    @Override
    public void batchSave(List<QuestionStat> stats) {
        MybatisBatchUtil.batchSave(sqlSessionFactory, QuestionStatMapper.class, stats);
    }
}
