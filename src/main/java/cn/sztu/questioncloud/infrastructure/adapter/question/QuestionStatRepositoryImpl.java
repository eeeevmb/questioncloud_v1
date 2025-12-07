package cn.sztu.questioncloud.infrastructure.adapter.question;

import cn.sztu.questioncloud.application.question.port.QuestionStatRepository;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.QuestionStat;
import cn.sztu.questioncloud.infrastructure.common.persistent.mapper.question.QuestionStatMapper;
import cn.xbatis.core.mybatis.MybatisBatchUtil;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class QuestionStatRepositoryImpl implements QuestionStatRepository {
    private final QuestionStatMapper questionStatMapper;
    private final SqlSessionFactory sqlSessionFactory;

    public QuestionStatRepositoryImpl(QuestionStatMapper questionStatMapper, SqlSessionFactory sqlSessionFactory) {
        this.questionStatMapper = questionStatMapper;
        this.sqlSessionFactory = sqlSessionFactory;
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
