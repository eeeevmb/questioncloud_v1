package cn.sztu.questioncloud.application.question.service.impl;

import cn.sztu.questioncloud.application.ai.dto.QuestionSemanticEnrichmentDTO;
import cn.sztu.questioncloud.application.ai.port.LlmPort;
import cn.sztu.questioncloud.application.common.enums.VectorDocTypeEnum;
import cn.sztu.questioncloud.application.common.port.VectorPort;
import cn.sztu.questioncloud.application.question.messaging.QuestionEventMessage;
import cn.sztu.questioncloud.application.question.port.QuestionStatRepository;
import cn.sztu.questioncloud.application.question.port.QuestionVersionRepository;
import cn.sztu.questioncloud.application.question.service.QuestionVectorizeService;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.QuestionStat;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.QuestionVersionEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class QuestionVectorizeServiceImpl implements QuestionVectorizeService {

    private final VectorPort vectorPort;
    private final QuestionVersionRepository questionVersionRepository;
    private final QuestionStatRepository questionStatRepository;
    private final LlmPort llmPort;

    @Override
    public void onQuestionUpsert(QuestionEventMessage message) {
        QuestionVersionEntity versionEntity = questionVersionRepository.getVersionById(message.getVersionId());
        QuestionStat questionStat = questionStatRepository.getByQuestionId(message.getQuestionId());

        String vectorId = "Q_" + message.getQuestionId();
        String text = "题干：\n" + versionEntity.getStem()
                + "\n\n解析：\n" + versionEntity.getSolution();

        QuestionSemanticEnrichmentDTO dto = llmPort.generateQuestionSemanticEnrichmentDTO(text);
        log.info("题目语义增强结果: {}", dto);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("docType", VectorDocTypeEnum.QUESTION.getCode());
        metadata.put("questionId", message.getQuestionId());
        metadata.put("versionId", message.getVersionId());
        metadata.put("collectionId", message.getCollectionId());
        metadata.put("typeCode", versionEntity.getTypeCode());
        metadata.put("difficulty", questionStat.getDifficulty() == null ? 0.00 : questionStat.getDifficulty());
        metadata.put("ownerId", message.getOwnerId());

        vectorPort.upsert(vectorId, dto.toString(), metadata);
    }

    @Override
    public void onQuestionDeleted(QuestionEventMessage message) {
        String vectorId = "Q_" + message.getQuestionId();
        vectorPort.delete(vectorId);
    }
}
