package cn.sztu.questioncloud.application.knowledge_point.port;

import cn.sztu.questioncloud.infrastructure.common.persistent.entity.knowledge_point.KnowledgePointEntity;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.knowledge_point.TextbookKnowledgeRelEntity;

import java.util.List;

public interface TextbookKnowledgeRelRepository {

    List<TextbookKnowledgeRelEntity> listByTextbookId(Long textbookId);

    List<KnowledgePointEntity> getByTextbookId(Long textbookId);

    void save(TextbookKnowledgeRelEntity entity);

    void deleteByTextbookId(Long textbookId);

    boolean existsByKnowledgePointId(Long knowledgePointId);

    void delete(Long id);
}
