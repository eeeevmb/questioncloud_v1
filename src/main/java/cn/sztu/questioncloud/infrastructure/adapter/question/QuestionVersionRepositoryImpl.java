package cn.sztu.questioncloud.infrastructure.adapter.question;

import cn.sztu.questioncloud.application.question.port.QuestionVersionRepository;
import cn.sztu.questioncloud.infrastructure.common.id.HutoolSnowflakeIdGenerator;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.QuestionVersionEntity;
import cn.sztu.questioncloud.infrastructure.common.persistent.mapper.question.QuestionVersionMapper;
import cn.xbatis.core.mybatis.MybatisBatchUtil;
import cn.xbatis.core.sql.executor.SubQuery;
import cn.xbatis.core.sql.executor.chain.DeleteChain;
import cn.xbatis.core.sql.executor.chain.QueryChain;
import db.sql.api.cmd.basic.IDataset;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.stereotype.Repository;
import static db.sql.api.impl.cmd.Methods.*;

import java.util.*;
import java.util.stream.Collectors;

@Repository
public class QuestionVersionRepositoryImpl implements QuestionVersionRepository {
    private final QuestionVersionMapper questionVersionMapper;
    private final SqlSessionFactory sqlSessionFactory;

    public QuestionVersionRepositoryImpl(QuestionVersionMapper questionVersionMapper, SqlSessionFactory sqlSessionFactory) {
        this.questionVersionMapper = questionVersionMapper;
        this.sqlSessionFactory = sqlSessionFactory;
    }

    /**
     * 根据题目ID查询最新题目版本实体
     *
     * @param questionId 题目ID
     * @return 最新版本实体
     */
    @Override
    public QuestionVersionEntity getCurrentVersionByQuestionId(Long questionId) {
        return QueryChain.of(questionVersionMapper)
                .eq(QuestionVersionEntity::getQuestionId, questionId)
                .orderByDesc(QuestionVersionEntity::getVersionNo)
                .limit(1)
                .get();
    }

    /**
     * 根据题目ID批量查询最新版本记录表
     *
     * @param questionIds 题目ID
     * @return 题目版本表
     */
    @Override
    public Map<Long, QuestionVersionEntity> getCurrentVersionsByQuestionIds(Collection<Long> questionIds) {
        SubQuery subQuery = SubQuery.create("sub")
                .select(QuestionVersionEntity::getQuestionId)
                .select(QuestionVersionEntity::getVersionNo, c -> max(c).as("max_version_no"))
                .from(QuestionVersionEntity.class)
                .in(QuestionVersionEntity::getQuestionId, questionIds)
                .groupBy(QuestionVersionEntity::getQuestionId);
        return QueryChain.of(questionVersionMapper)
                .join(QuestionVersionEntity.class, subQuery, on -> on
                        .eq(QuestionVersionEntity::getQuestionId, subQuery.$outerField(QuestionVersionEntity::getQuestionId))
                        .eq(QuestionVersionEntity::getVersionNo,  subQuery.$outerField("max_version_no")))
                .mapWithKey(QuestionVersionEntity::getQuestionId);
    }

    /**
     * 根据版本ID查询题目版本记录
     *
     * @param versionId 题目版本ID
     * @return 题目实体版本
     */
    @Override
    public QuestionVersionEntity getVersionById(Long versionId) {
        return QueryChain.of(questionVersionMapper)
                .eq(QuestionVersionEntity::getId, versionId)
                .limit(1)
                .get();
    }

    /**
     * 根据题目ID删除所有题目版本记录
     *
     * @param questionId 题目ID
     */
    @Override
    public void deleteByQuestionId(Long questionId) {
        DeleteChain.of(questionVersionMapper)
                .eq(QuestionVersionEntity::getQuestionId, questionId)
                .execute();
    }

    /**
     * 批量查询存在的版本ID列表
     *
     * @param ids 待检查的版本ID集合
     * @return 数据库中实际存在的ID列表
     */
    @Override
    public List<Long> findExistingIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }

        return QueryChain.of(questionVersionMapper)
                .select(QuestionVersionEntity::getId)
                .in(QuestionVersionEntity::getId, ids)
                .list()
                .stream()
                .map(QuestionVersionEntity::getId)
                .collect(Collectors.toList());
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
