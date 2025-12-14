package cn.sztu.questioncloud.infrastructure.adapter.question;

import cn.sztu.questioncloud.application.question.port.QuestionRepository;
import cn.sztu.questioncloud.infrastructure.common.id.HutoolSnowflakeIdGenerator;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.QuestionEntity;
import cn.sztu.questioncloud.infrastructure.common.persistent.mapper.question.QuestionMapper;
import cn.xbatis.core.mybatis.MybatisBatchUtil;
import cn.xbatis.core.sql.executor.chain.QueryChain;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class QuestionRepositoryImpl implements QuestionRepository {
    private final QuestionMapper questionMapper;
    private final SqlSessionFactory sqlSessionFactory;

    public QuestionRepositoryImpl(QuestionMapper questionMapper, SqlSessionFactory sqlSessionFactory) {
        this.questionMapper = questionMapper;
        this.sqlSessionFactory = sqlSessionFactory;
    }

    /**
     * 根据ID查找题目实体
     *
     * @param questionId 题目ID
     * @return 题目实体
     */
    @Override
    public QuestionEntity getById(Long questionId) {
        return QueryChain.of(questionMapper)
                .eq(QuestionEntity::getId, questionId)
                .limit(1)
                .get();
    }

    /**
     * 根据实体更新题目
     *
     * @param questionEntity 题目实体
     */
    @Override
    public void update(QuestionEntity questionEntity) {
        questionMapper.update(questionEntity);
    }

    /**
     * 保存题目实体
     *
     * @param entity 题目实体
     */
    @Override
    public void save(QuestionEntity entity) {
        if (entity.getId() == null) {
            entity.setId(HutoolSnowflakeIdGenerator.generateLongId());
        }
        questionMapper.save(entity);
    }

    /**
     * 批量保存题目实体
     *
     * @param entities 实体列表
     */
    @Override
    public void batchSave(List<QuestionEntity> entities) {
        MybatisBatchUtil.batchSave(sqlSessionFactory, QuestionMapper.class, entities);
    }
}
