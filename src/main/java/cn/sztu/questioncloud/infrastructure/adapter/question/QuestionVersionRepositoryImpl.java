package cn.sztu.questioncloud.infrastructure.adapter.question;

import cn.sztu.questioncloud.application.question.port.QuestionVersionRepository;
import cn.sztu.questioncloud.infrastructure.common.id.HutoolSnowflakeIdGenerator;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.QuestionVersionEntity;
import cn.sztu.questioncloud.infrastructure.common.persistent.mapper.question.QuestionVersionMapper;
import cn.xbatis.core.mybatis.MybatisBatchUtil;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class QuestionVersionRepositoryImpl implements QuestionVersionRepository {
    private final QuestionVersionMapper questionVersionMapper;
    private final SqlSessionFactory sqlSessionFactory;

    public QuestionVersionRepositoryImpl(QuestionVersionMapper questionVersionMapper, SqlSessionFactory sqlSessionFactory) {
        this.questionVersionMapper = questionVersionMapper;
        this.sqlSessionFactory = sqlSessionFactory;
    }

    /**
     * 保存题目版本实体
     *
     * @param entity 题目版本实体
     * @return 题目版本ID
     */
    @Override
    public Optional<Long> save(QuestionVersionEntity entity) {
        if (entity.getId() == null) {
            entity.setId(HutoolSnowflakeIdGenerator.generateLongId());
        }
        questionVersionMapper.save(entity);
        return Optional.ofNullable(entity.getId());
    }

    /**
     * 批量保存题目版本实体
     *
     * @param entities 题目版本实体
     */
    @Override
    public void batchSave(List<QuestionVersionEntity> entities) {
        MybatisBatchUtil.batchSave(sqlSessionFactory, QuestionVersionMapper.class, entities);
    }
}
