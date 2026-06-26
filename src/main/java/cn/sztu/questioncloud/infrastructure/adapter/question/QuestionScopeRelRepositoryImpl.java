package cn.sztu.questioncloud.infrastructure.adapter.question;

import cn.sztu.questioncloud.application.question.port.QuestionScopeRelRepository;
import cn.sztu.questioncloud.infrastructure.common.id.HutoolSnowflakeIdGenerator;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.QuestionScopeRelEntity;
import cn.sztu.questioncloud.infrastructure.common.persistent.mapper.question.QuestionScopeRelMapper;
import cn.xbatis.core.sql.executor.chain.DeleteChain;
import cn.xbatis.core.sql.executor.chain.QueryChain;
import cn.xbatis.core.sql.executor.chain.UpdateChain;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class QuestionScopeRelRepositoryImpl implements QuestionScopeRelRepository {

    private final QuestionScopeRelMapper questionScopeRelMapper;

    @Override
    public QuestionScopeRelEntity getById(Long id) {
        return QueryChain.of(questionScopeRelMapper)
                .eq(QuestionScopeRelEntity::getId, id)
                .get();
    }

    @Override
    public void save(QuestionScopeRelEntity entity) {
        if (entity.getId() == null) {
            entity.setId(HutoolSnowflakeIdGenerator.generateLongId());
        }
        questionScopeRelMapper.save(entity);
    }

    @Override
    public void update(QuestionScopeRelEntity entity) {
        UpdateChain.of(questionScopeRelMapper)
                .update(QuestionScopeRelEntity.class)
                .set(QuestionScopeRelEntity::getQuestionId, entity.getQuestionId())
                .set(QuestionScopeRelEntity::getKnowledgeScopeId, entity.getKnowledgeScopeId())
                .set(QuestionScopeRelEntity::getCreatedAt, entity.getCreatedAt())
                .set(QuestionScopeRelEntity::getUpdatedAt, entity.getUpdatedAt())
                .eq(QuestionScopeRelEntity::getId, entity.getId())
                .execute();
    }

    @Override
    public void delete(Long id) {
        DeleteChain.of(questionScopeRelMapper)
                .eq(QuestionScopeRelEntity::getId, id)
                .execute();
    }
}
