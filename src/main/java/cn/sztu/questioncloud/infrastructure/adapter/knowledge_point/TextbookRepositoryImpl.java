package cn.sztu.questioncloud.infrastructure.adapter.knowledge_point;

import cn.sztu.questioncloud.application.knowledge_point.dto.TextbookCheckDTO;
import cn.sztu.questioncloud.application.knowledge_point.port.TextbookRepository;
import cn.sztu.questioncloud.infrastructure.common.id.HutoolSnowflakeIdGenerator;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.knowledge_point.TextbookEntity;
import cn.sztu.questioncloud.infrastructure.common.persistent.mapper.knowledge_point.TextbookMapper;
import cn.xbatis.core.sql.executor.chain.DeleteChain;
import cn.xbatis.core.sql.executor.chain.QueryChain;
import cn.xbatis.core.sql.executor.chain.UpdateChain;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class TextbookRepositoryImpl implements TextbookRepository {

    private final TextbookMapper textbookMapper;

    @Override
    public TextbookEntity getById(Long id) {
        return QueryChain.of(textbookMapper)
                .eq(TextbookEntity::getId, id)
                .get();
    }

    @Override
    public void save(TextbookEntity entity) {
        if (entity.getId() == null) {
            entity.setId(HutoolSnowflakeIdGenerator.generateLongId());
        }
        textbookMapper.save(entity);
    }

    @Override
    public void update(TextbookEntity entity) {
        UpdateChain.of(textbookMapper)
                .update(TextbookEntity.class)
                .set(TextbookEntity::getKnowledgeScopeId, entity.getKnowledgeScopeId())
                .set(TextbookEntity::getTextbookName, entity.getTextbookName())
                .set(TextbookEntity::getEdition, entity.getEdition())
                .set(TextbookEntity::getAuthor, entity.getAuthor())
                .set(TextbookEntity::getPublisher, entity.getPublisher())
                .set(TextbookEntity::getIsbn, entity.getIsbn())
                .set(TextbookEntity::getCreatedBy, entity.getCreatedBy())
                .set(TextbookEntity::getCreatedAt, entity.getCreatedAt())
                .set(TextbookEntity::getUpdatedAt, entity.getUpdatedAt())
                .eq(TextbookEntity::getId, entity.getId())
                .execute();
    }

    @Override
    public void delete(Long id) {
        DeleteChain.of(textbookMapper)
                .eq(TextbookEntity::getId, id)
                .execute();
    }

    @Override
    public TextbookEntity getDuplicate(TextbookCheckDTO duplicateCheckDTO) {
        QueryChain<TextbookEntity> queryChain = QueryChain.of(textbookMapper)
                .eq(TextbookEntity::getKnowledgeScopeId, duplicateCheckDTO.getKnowledgeScopeId())
                .eq(TextbookEntity::getTextbookName, duplicateCheckDTO.getTextbookName());
        if (duplicateCheckDTO.getAuthor() != null && !duplicateCheckDTO.getAuthor().isBlank()) {
            queryChain.eq(TextbookEntity::getAuthor, duplicateCheckDTO.getAuthor().trim());
        }
        return queryChain.get();
    }
}
